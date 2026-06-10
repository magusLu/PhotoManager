package com.photomaster.app.data.transfer

import android.content.ContentResolver
import android.content.Context
import android.graphics.Bitmap
import android.os.Build
import android.util.Size
import com.photomaster.app.domain.model.MediaItem
import fi.iki.elonen.NanoHTTPD
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.InputStream

/**
 * 局域网传输 HTTP 服务器，运行在手机端。
 *
 * 端点：
 *  GET /              → HTML 图库页面（浏览器访问）
 *  GET /api/items     → JSON 图片列表
 *  GET /photo/{id}    → 原图流（下载）
 *  GET /thumb/{id}    → 缩略图流（320×320）
 */
class LanTransferServer(
    port: Int,
    private val context: Context,
    /** 每次请求时动态拉取最新媒体列表 */
    private val getItems: () -> List<MediaItem>,
) : NanoHTTPD(port) {

    private val resolver: ContentResolver get() = context.contentResolver

    override fun serve(session: IHTTPSession): Response {
        return when {
            session.uri == "/"             -> serveHtml()
            session.uri == "/api/items"    -> serveJson()
            session.uri.startsWith("/photo/") ->
                servePhoto(session.uri.removePrefix("/photo/").toLongOrNull())
            session.uri.startsWith("/thumb/") ->
                serveThumb(session.uri.removePrefix("/thumb/").toLongOrNull())
            else -> newFixedLengthResponse(
                Response.Status.NOT_FOUND, MIME_PLAINTEXT, "Not found"
            )
        }
    }

    // ── HTML gallery ─────────────────────────────────────────────────────────

    private fun serveHtml(): Response {
        val html = buildGalleryHtml()
        return newFixedLengthResponse(Response.Status.OK, "text/html; charset=utf-8", html)
    }

    // ── JSON list ─────────────────────────────────────────────────────────────

    private fun serveJson(): Response {
        val items = getItems()
        val json = buildString {
            append("[")
            items.forEachIndexed { idx, item ->
                if (idx > 0) append(",")
                append(
                    """{"id":${item.id},"name":${jsonStr(item.displayName)},""" +
                    """"mime":${jsonStr(item.mimeType)},"date":${item.dateTaken},""" +
                    """"isVideo":${item.isVideo}}"""
                )
            }
            append("]")
        }
        return newFixedLengthResponse(Response.Status.OK, "application/json", json)
    }

    // ── Photo stream (download) ───────────────────────────────────────────────

    private fun servePhoto(id: Long?): Response {
        if (id == null) return notFound()
        val item = getItems().find { it.id == id } ?: return notFound()
        return try {
            val stream: InputStream = resolver.openInputStream(item.uri) ?: return notFound()
            val mime = item.mimeType.ifBlank { "application/octet-stream" }
            newChunkedResponse(Response.Status.OK, mime, stream).also {
                val safeName = item.displayName
                    .replace("\"", "\\\"")
                    .replace("\n", "")
                it.addHeader("Content-Disposition", "attachment; filename=\"$safeName\"")
                it.addHeader("Access-Control-Allow-Origin", "*")
            }
        } catch (e: Exception) {
            notFound()
        }
    }

    // ── Thumbnail stream ──────────────────────────────────────────────────────

    private fun serveThumb(id: Long?): Response {
        if (id == null) return notFound()
        val item = getItems().find { it.id == id } ?: return notFound()
        return try {
            val bytes: ByteArray = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val bmp = resolver.loadThumbnail(item.uri, Size(320, 320), null)
                ByteArrayOutputStream().also { out ->
                    bmp.compress(Bitmap.CompressFormat.JPEG, 80, out)
                    bmp.recycle()
                }.toByteArray()
            } else {
                // API < 29 fallback: stream full image (browser will scale it)
                ByteArrayOutputStream().also { out ->
                    resolver.openInputStream(item.uri)?.use { it.copyTo(out) }
                }.toByteArray()
            }
            newFixedLengthResponse(
                Response.Status.OK, "image/jpeg",
                ByteArrayInputStream(bytes), bytes.size.toLong()
            ).also {
                it.addHeader("Cache-Control", "max-age=3600")
                it.addHeader("Access-Control-Allow-Origin", "*")
            }
        } catch (e: Exception) {
            notFound()
        }
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private fun notFound() =
        newFixedLengthResponse(Response.Status.NOT_FOUND, MIME_PLAINTEXT, "Not found")

    private fun jsonStr(s: String) =
        "\"${s.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n")}\""

    // ── Embedded HTML gallery ─────────────────────────────────────────────────

    private fun buildGalleryHtml(): String = """<!DOCTYPE html>
<html lang="zh">
<head>
<meta charset="UTF-8">
<meta name="viewport" content="width=device-width,initial-scale=1">
<title>PhotoMaster 传输</title>
<style>
*{box-sizing:border-box;margin:0;padding:0}
body{font-family:-apple-system,BlinkMacSystemFont,"Segoe UI",sans-serif;background:#111;color:#eee;min-height:100vh}
header{padding:14px 20px;background:#1a1a2e;display:flex;align-items:center;justify-content:space-between}
header h1{font-size:1.1rem;font-weight:600;display:flex;align-items:center;gap:8px}
header .sub{font-size:.8rem;color:#888;margin-top:2px}
#toolbar{padding:8px 12px;background:#161622;display:flex;gap:8px;flex-wrap:wrap;align-items:center;border-bottom:1px solid #222}
.btn{padding:6px 14px;border:none;border-radius:6px;cursor:pointer;font-size:.82rem;transition:.15s}
.btn-primary{background:#5c6bc0;color:#fff}.btn-primary:hover{background:#4a58a8}
.btn-danger{background:#c0453c;color:#fff}.btn-danger:hover{background:#a83830}
.btn-outline{background:transparent;border:1px solid #444;color:#ccc}.btn-outline:hover{background:#222}
#selcount{font-size:.8rem;color:#7986cb;margin-left:4px;min-width:60px}
#status{font-size:.8rem;color:#555;margin-left:auto}
#grid{display:grid;grid-template-columns:repeat(auto-fill,minmax(150px,1fr));gap:3px;padding:6px}
.cell{position:relative;aspect-ratio:1;overflow:hidden;border-radius:4px;cursor:pointer;background:#1e1e2e;user-select:none}
.cell img{width:100%;height:100%;object-fit:cover;transition:.18s}
.cell:hover img{transform:scale(1.06)}
.cell .chk{position:absolute;top:5px;left:5px;width:20px;height:20px;border-radius:50%;border:2px solid rgba(255,255,255,.7);background:rgba(0,0,0,.3);display:flex;align-items:center;justify-content:center;font-size:11px;transition:.15s;opacity:0}
.cell:hover .chk,.cell.sel .chk{opacity:1}
.cell.sel .chk{background:#5c6bc0;border-color:#5c6bc0;color:#fff}
.cell .vid-badge{position:absolute;bottom:5px;right:5px;background:rgba(0,0,0,.6);color:#fff;font-size:.65rem;padding:1px 5px;border-radius:3px}
.cell .name{position:absolute;bottom:0;left:0;right:0;padding:3px 6px;background:linear-gradient(transparent,rgba(0,0,0,.75));font-size:.65rem;white-space:nowrap;overflow:hidden;text-overflow:ellipsis;opacity:0;transition:.15s}
.cell:hover .name{opacity:1}
#lb{display:none;position:fixed;inset:0;background:rgba(0,0,0,.92);z-index:200;flex-direction:column;align-items:center;justify-content:center;gap:10px}
#lb.open{display:flex}
#lb img,#lb video{max-width:92vw;max-height:78vh;border-radius:6px;object-fit:contain}
#lb .lb-bar{display:flex;gap:8px;align-items:center}
#lb .lb-name{font-size:.78rem;color:#aaa;max-width:300px;overflow:hidden;text-overflow:ellipsis;white-space:nowrap}
#empty,#loading{display:none;text-align:center;padding:80px 20px;color:#555;font-size:.95rem}
</style>
</head>
<body>
<header>
  <div>
    <h1>📷 PhotoMaster 传输</h1>
    <div class="sub" id="subtitle">正在加载…</div>
  </div>
</header>
<div id="toolbar">
  <button class="btn btn-outline" onclick="toggleAll()">全选 / 取消</button>
  <button class="btn btn-primary" onclick="dlSelected()">⬇ 下载选中</button>
  <button class="btn btn-primary" onclick="dlAll()">⬇ 全部下载</button>
  <span id="selcount"></span>
  <span id="status"></span>
</div>
<div id="loading">加载中…</div>
<div id="empty">暂无图片</div>
<div id="grid"></div>

<div id="lb">
  <div id="lb-media"></div>
  <div class="lb-bar">
    <span class="lb-name" id="lb-name"></span>
    <button class="btn btn-primary" id="lb-dl">⬇ 下载</button>
    <button class="btn btn-outline" onclick="closeLb()">✕ 关闭</button>
  </div>
</div>

<script>
let items = [];
let sel = new Set();
let dlQueue = [];
let dlIdx = 0;

async function load() {
  document.getElementById('loading').style.display = 'block';
  try {
    const r = await fetch('/api/items');
    items = await r.json();
  } catch(e) {
    document.getElementById('loading').textContent = '加载失败，请刷新';
    return;
  }
  document.getElementById('loading').style.display = 'none';
  document.getElementById('subtitle').textContent =
    '局域网传输 · ' + items.length + ' 个文件';
  if (!items.length) { document.getElementById('empty').style.display = 'block'; return; }
  const grid = document.getElementById('grid');
  grid.innerHTML = '';
  items.forEach(item => {
    const cell = document.createElement('div');
    cell.className = 'cell';
    cell.dataset.id = item.id;
    const thumb = item.isVideo
      ? '<img loading="lazy" src="/thumb/'+item.id+'" alt=""><span class="vid-badge">视频</span>'
      : '<img loading="lazy" src="/thumb/'+item.id+'" alt="">';
    cell.innerHTML = '<div class="chk">✓</div>' + thumb +
      '<span class="name">' + esc(item.name) + '</span>';
    cell.addEventListener('click', e => {
      if (e.shiftKey || e.ctrlKey || e.metaKey) { toggleSel(item.id, cell); }
      else { openLb(item); }
    });
    cell.addEventListener('contextmenu', e => { e.preventDefault(); toggleSel(item.id, cell); });
    grid.appendChild(cell);
  });
  updCount();
}

function toggleSel(id, cell) {
  if (sel.has(id)) { sel.delete(id); cell.classList.remove('sel'); }
  else { sel.add(id); cell.classList.add('sel'); }
  updCount();
}

function toggleAll() {
  if (sel.size === items.length) {
    sel.clear();
    document.querySelectorAll('.cell').forEach(c => c.classList.remove('sel'));
  } else {
    items.forEach(i => sel.add(i.id));
    document.querySelectorAll('.cell').forEach(c => c.classList.add('sel'));
  }
  updCount();
}

function updCount() {
  document.getElementById('selcount').textContent = sel.size ? sel.size + ' 已选' : '';
}

function dlSelected() {
  const ids = sel.size ? [...sel] : [];
  if (!ids.length) { alert('请先选择图片（长按或 Ctrl+点击）'); return; }
  batchDl(ids);
}

function dlAll() { batchDl(items.map(i => i.id)); }

function batchDl(ids) {
  let i = 0;
  const st = document.getElementById('status');
  function next() {
    if (i >= ids.length) { st.textContent = '下载完成 ✓'; return; }
    st.textContent = '下载 ' + (i+1) + '/' + ids.length + '…';
    const a = document.createElement('a');
    a.href = '/photo/' + ids[i];
    a.download = '';
    document.body.appendChild(a);
    a.click();
    document.body.removeChild(a);
    i++;
    setTimeout(next, 400);
  }
  next();
}

function openLb(item) {
  const media = document.getElementById('lb-media');
  media.innerHTML = item.isVideo
    ? '<video src="/photo/'+item.id+'" controls autoplay style="max-width:92vw;max-height:78vh;border-radius:6px"></video>'
    : '<img src="/photo/'+item.id+'" alt="">';
  document.getElementById('lb-name').textContent = item.name;
  document.getElementById('lb-dl').onclick = () => {
    const a = document.createElement('a'); a.href='/photo/'+item.id; a.download=item.name; a.click();
  };
  document.getElementById('lb').classList.add('open');
}

function closeLb() {
  document.getElementById('lb').classList.remove('open');
  document.getElementById('lb-media').innerHTML = '';
}

function esc(s) {
  return s.replace(/&/g,'&amp;').replace(/</g,'&lt;').replace(/>/g,'&gt;').replace(/"/g,'&quot;');
}

document.getElementById('lb').addEventListener('click', e => {
  if (e.target === e.currentTarget) closeLb();
});
document.addEventListener('keydown', e => { if (e.key === 'Escape') closeLb(); });

load();
</script>
</body>
</html>""".trimIndent()
}
