import os
import re
import subprocess
import sys

CHROME_PATHS = [
    r"C:\Program Files\Google\Chrome\Application\chrome.exe",
    r"C:\Program Files (x86)\Microsoft\Edge\Application\msedge.exe"
]

browser_exe = None
for p in CHROME_PATHS:
    if os.path.exists(p):
        browser_exe = p
        break

if not browser_exe:
    print("Neither Chrome nor Edge found!")
    sys.exit(1)

print(f"Using browser for PDF rendering: {browser_exe}")

CSS = """
body {
    font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, Helvetica, Arial, sans-serif;
    color: #1f2937;
    line-height: 1.6;
    padding: 30px 45px;
    font-size: 13px;
    max-width: 900px;
    margin: 0 auto;
}
h1 {
    font-size: 24px;
    border-bottom: 2px solid #2563eb;
    padding-bottom: 8px;
    color: #111827;
    margin-top: 10px;
}
h2 {
    font-size: 18px;
    border-bottom: 1px solid #e5e7eb;
    padding-bottom: 5px;
    color: #1f2937;
    margin-top: 24px;
}
h3 {
    font-size: 14px;
    color: #374151;
    margin-top: 16px;
}
table {
    width: 100%;
    border-collapse: collapse;
    margin: 15px 0;
    font-size: 11.5px;
}
th, td {
    border: 1px solid #d1d5db;
    padding: 6px 10px;
    text-align: left;
}
th {
    background-color: #f3f4f6;
    font-weight: 600;
    color: #111827;
}
tr:nth-child(even) td {
    background-color: #f9fafb;
}
pre {
    background-color: #f8fafc;
    border: 1px solid #e2e8f0;
    border-radius: 6px;
    padding: 10px 14px;
    font-family: "JetBrains Mono", Consolas, Courier, monospace;
    font-size: 11px;
    overflow-x: auto;
    white-space: pre-wrap;
    word-break: break-word;
}
code {
    background-color: #f1f5f9;
    padding: 2px 5px;
    border-radius: 4px;
    font-family: "JetBrains Mono", Consolas, monospace;
    font-size: 11px;
}
pre code {
    background-color: transparent;
    padding: 0;
}
blockquote {
    border-left: 4px solid #2563eb;
    margin: 12px 0;
    padding: 6px 14px;
    background-color: #eff6ff;
    color: #1e40af;
}
hr {
    border: none;
    border-top: 1px solid #e5e7eb;
    margin: 20px 0;
}
ul, ol {
    margin: 10px 0 10px 22px;
}
li {
    margin-bottom: 4px;
}
.header-tag {
    display: inline-block;
    background: #eff6ff;
    color: #2563eb;
    padding: 2px 8px;
    border-radius: 4px;
    font-weight: 600;
    font-size: 11px;
    margin-bottom: 10px;
}
"""

def md_to_html(md_text):
    lines = md_text.splitlines()
    html_lines = []
    in_code_block = False
    code_lang = ""
    code_content = []
    in_table = False
    table_lines = []

    def flush_table():
        nonlocal in_table, table_lines, html_lines
        if not table_lines:
            return
        html_lines.append("<table>")
        is_first = True
        for tl in table_lines:
            cells = [c.strip() for c in tl.strip().strip("|").split("|")]
            if all(re.match(r"^:?-+:?$", c) for c in cells if c):
                continue  # Header separator row
            tag = "th" if is_first else "td"
            row_html = "<tr>" + "".join(f"<{tag}>{format_inline(c)}</{tag}>" for c in cells) + "</tr>"
            html_lines.append(row_html)
            is_first = False
        html_lines.append("</table>")
        table_lines = []
        in_table = False

    def format_inline(text):
        # bold
        text = re.sub(r"\*\*(.+?)\*\*", r"<strong>\1</strong>", text)
        # code
        text = re.sub(r"`(.+?)`", r"<code>\1</code>", text)
        # italic
        text = re.sub(r"\*(.+?)\*", r"<em>\1</em>", text)
        return text

    for line in lines:
        stripped = line.strip()

        # Handle Code blocks
        if stripped.startswith("```"):
            if in_code_block:
                html_lines.append(f"<pre><code>{'<br>'.join(code_content)}</code></pre>")
                in_code_block = False
                code_content = []
            else:
                if in_table:
                    flush_table()
                in_code_block = True
                code_lang = stripped[3:].strip()
            continue

        if in_code_block:
            code_content.append(line.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;"))
            continue

        # Handle Tables
        if stripped.startswith("|") and stripped.endswith("|"):
            in_table = True
            table_lines.append(line)
            continue
        elif in_table:
            flush_table()

        # Headers
        if line.startswith("# "):
            html_lines.append(f"<h1>{format_inline(line[2:].strip())}</h1>")
        elif line.startswith("## "):
            html_lines.append(f"<h2>{format_inline(line[3:].strip())}</h2>")
        elif line.startswith("### "):
            html_lines.append(f"<h3>{format_inline(line[4:].strip())}</h3>")
        elif line.startswith("---"):
            html_lines.append("<hr>")
        elif stripped.startswith("- "):
            html_lines.append(f"<li>{format_inline(stripped[2:])}</li>")
        elif stripped.startswith("1. ") or stripped.startswith("2. ") or stripped.startswith("3. ") or stripped.startswith("4. ") or stripped.startswith("5. "):
            html_lines.append(f"<li>{format_inline(stripped[3:])}</li>")
        elif stripped:
            html_lines.append(f"<p>{format_inline(line)}</p>")

    if in_table:
        flush_table()
    if in_code_block:
        html_lines.append(f"<pre><code>{'<br>'.join(code_content)}</code></pre>")

    return f"""<!DOCTYPE html>
<html>
<head>
<meta charset="utf-8">
<style>{CSS}</style>
</head>
<body>
{''.join(html_lines)}
</body>
</html>"""

def convert_md_to_pdf(md_path, pdf_path):
    print(f"Converting {md_path} -> {pdf_path}...")
    with open(md_path, "r", encoding="utf-8") as f:
        md_text = f.read()

    html_content = md_to_html(md_text)
    temp_html = md_path.replace(".md", ".temp.html")
    with open(temp_html, "w", encoding="utf-8") as f:
        f.write(html_content)

    abs_temp_html = os.path.abspath(temp_html)
    abs_pdf = os.path.abspath(pdf_path)

    cmd = [
        browser_exe,
        "--headless",
        "--disable-gpu",
        "--run-all-compositor-stages-before-draw",
        f"--print-to-pdf={abs_pdf}",
        f"file:///{abs_temp_html.replace(os.sep, '/')}"
    ]

    res = subprocess.run(cmd, capture_output=True, text=True)
    if os.path.exists(temp_html):
        os.remove(temp_html)

    if os.path.exists(pdf_path) and os.path.getsize(pdf_path) > 0:
        print(f"SUCCESS: Generated {pdf_path} ({os.path.getsize(pdf_path)} bytes)")
    else:
        print(f"ERROR generating {pdf_path}: {res.stderr}")

if __name__ == "__main__":
    docs = [
        ("docs/ARCHITECTURE_AND_SCALE_DESIGN.md", "docs/ARCHITECTURE_AND_SCALE_DESIGN.pdf"),
        ("docs/METRICS_AND_EVALUATION.md", "docs/METRICS_AND_EVALUATION.pdf"),
        ("docs/RUN_INSTRUCTIONS_VERIFIED.md", "docs/RUN_INSTRUCTIONS_VERIFIED.pdf"),
    ]
    for md, pdf in docs:
        if os.path.exists(md):
            convert_md_to_pdf(md, pdf)
