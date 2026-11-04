(function () {
  'use strict';

  const escapeHtml = (value) => String(value ?? '').replace(/[&<>"']/g, (char) => ({
    '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;'
  })[char]);
  const safeUrl = (value) => /^https?:\/\//i.test(String(value || '')) || /^mailto:/i.test(String(value || ''));

  function createRenderer() {
    const renderer = new marked.Renderer();
    renderer.html = (html) => escapeHtml(html);
    renderer.link = (href, title, text) => {
      if (!safeUrl(href)) return text;
      const titleAttr = title ? ` title="${escapeHtml(title)}"` : '';
      return `<a href="${escapeHtml(href)}"${titleAttr}>${text}</a>`;
    };
    renderer.image = (_href, _title, alt) => `<span>[${escapeHtml(alt || 'image')}]</span>`;
    return renderer;
  }

  function prepareMarkdown(source) {
    return String(source || '').replace(
      /^```(?:latex|tex|math)\s*\r?\n([\s\S]*?)\r?\n```\s*$/gim,
      (_match, body) => `\n$$\n${body}\n$$\n`
    );
  }

  async function renderMermaid(container) {
    const blocks = Array.from(container.querySelectorAll('pre code.language-mermaid'));
    for (let index = 0; index < blocks.length; index += 1) {
      const code = blocks[index];
      const pre = code.closest('pre');
      if (!pre) continue;
      const host = document.createElement('div');
      host.className = 'mermaid-box';
      try {
        const result = await mermaid.render(`ariai-mermaid-${Date.now()}-${index}`, code.textContent || '');
        host.innerHTML = result.svg;
        pre.replaceWith(host);
      } catch (_error) {
        pre.classList.add('render-error');
      }
    }
  }

  window.renderMarkdown = async function (source, textColor, linkColor, fontSizePx) {
    const root = document.getElementById('content');
    if (!root) return;
    const safeColor = /^#[0-9a-f]{6}$/i.test(textColor) ? textColor : '#222222';
    const safeLinkColor = /^#[0-9a-f]{6}$/i.test(linkColor) ? linkColor : '#3978e8';
    const size = Math.max(12, Math.min(28, Number(fontSizePx) || 16));
    document.documentElement.style.setProperty('--reply-color', safeColor);
    document.documentElement.style.setProperty('--link-color', safeLinkColor);
    document.body.style.fontSize = `${size}px`;

    try {
      const markdown = prepareMarkdown(source);
      root.innerHTML = marked.parse(markdown, {
        gfm: true,
        breaks: true,
        renderer: createRenderer()
      });
      mermaid.initialize({ startOnLoad: false, securityLevel: 'strict', theme: 'neutral' });
      await renderMermaid(root);
      renderMathInElement(root, {
        delimiters: [
          { left: '$$', right: '$$', display: true },
          { left: '\\[', right: '\\]', display: true },
          { left: '\\(', right: '\\)', display: false },
          { left: '$', right: '$', display: false }
        ],
        ignoredTags: ['script', 'noscript', 'style', 'textarea', 'pre', 'code'],
        throwOnError: false,
        strict: 'ignore'
      });
    } catch (_error) {
      root.textContent = String(source || '');
    }

    requestAnimationFrame(() => {
      setTimeout(() => {
        window.scrollTo(0, 0);
      }, 0);
    });
  };
})();
