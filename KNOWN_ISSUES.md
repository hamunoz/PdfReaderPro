# Known Issues

This document lists known issues and limitations in PDF Reader Pro, along with workarounds where available.

## PDF Rendering

### Large PDFs may cause slowdowns
**Issue:** PDFs with more than 500 pages or very large file sizes (>100MB) may experience slower scrolling and rendering.

**Workaround:**
- Use the page scrubber for faster navigation
- Split large PDFs into smaller files using the Split tool
- Close other apps to free up memory

**Status:** Under investigation

---

### Some PDF forms are not interactive
**Issue:** Complex PDF forms with JavaScript or advanced form fields may not be fully interactive.

**Workaround:** Use a desktop PDF editor for form filling.

**Status:** Limitation of PDF.js

---

### Certain fonts may not render correctly
**Issue:** PDFs using non-standard or custom embedded fonts may display with fallback fonts.

**Workaround:** If text appears incorrect, try opening the PDF on a desktop reader to verify the content.

**Status:** Limitation of PDF.js font handling

---

### Search or copy returns wrong text in some non-English PDFs
**Issue:** In some PDFs (often Indic scripts such as Hindi, Bengali, or Gujarati) the text renders correctly, but search, text selection, copy, and highlighting return incorrect or scrambled characters. Highlights are affected because they are made from a text selection, so a highlight may cover the right words on the page while listing the wrong text in the Highlights panel. This happens when the PDF's embedded fonts carry an incomplete or incorrect `/ToUnicode` map — the correct characters are simply not stored in the file, so no reader can recover them.

**Workaround:** Re-generate the PDF with a tool that embeds proper Unicode data (for example, "Print → Save as PDF" from Chrome). Note this affects every PDF viewer (including Adobe Reader and Chrome), not just this app.

**Status:** Limitation of the PDF file itself, not fixable in the reader (#40)

---

## Highlights

### Highlights already in a file may list slightly more or less text than they cover
**Issue:** Highlights that came with the PDF, whether exported from this app or added by another application, record where they are on the page but not which words they cover. The text shown for them in the Highlights panel is recovered by reading the page underneath, and the PDF text layer gives one run per line rather than per word, so the recovered text is estimated from where the highlight falls along the line. On unusual fonts or heavy kerning this can clip a word short or include a neighbouring one.

**Workaround:** None needed for reading; the highlight itself is drawn in exactly the right place. Only the text listed in the panel, and therefore what it matches in search, is approximate.

**Status:** Limitation of the data available from the PDF text layer. Highlights created in this app are unaffected, since their text is captured from the selection.

---

## PDF Tools

### Password-protected PDFs cannot be merged
**Issue:** The merge tool does not support password-protected PDF files.

**Workaround:** Use the Unlock tool to remove password protection first, then merge.

**Status:** By design (security consideration)

---

### Watermark positioning on rotated pages
**Issue:** Watermarks may appear in unexpected positions on pages that were previously rotated.

**Workaround:** Rotate pages to the correct orientation before adding watermarks.

**Status:** Known limitation

---

## File Access

### External storage permission required
**Issue:** On Android 10 and below, the app requires storage permissions to access PDF files. On Android 11+, the app uses scoped storage.

**Workaround:** Grant the requested permissions when prompted.

**Status:** Android platform requirement

---

### Some cloud storage files not accessible
**Issue:** PDFs opened from certain cloud storage apps may not be editable with PDF tools.

**Workaround:** Download the file locally first, then open it from the device storage.

**Status:** Android content provider limitation

---

## Performance

### Initial app launch may be slow
**Issue:** The first launch after installation may take a few seconds while initializing.

**Workaround:** Subsequent launches will be faster.

**Status:** Normal behavior for initial setup

---

### Memory usage with multiple tabs
**Issue:** Opening multiple large PDFs in quick succession may increase memory usage.

**Workaround:** Close PDFs when finished viewing to free memory.

**Status:** Under optimization

---

## Accessibility

### Some icons lack content descriptions
**Issue:** Not all decorative icons have accessibility labels, which may affect screen reader users.

**Workaround:** Use the text labels that accompany most controls.

**Status:** Being addressed in future updates

---

## Reporting New Issues

If you encounter an issue not listed here, please report it:

1. **Check existing issues:** [GitHub Issues](https://github.com/ahmmedrejowan/PdfReaderPro/issues)
2. **Create a new issue** with:
   - Device model and Android version
   - App version
   - Steps to reproduce
   - Expected vs actual behavior
   - Screenshots if applicable
   - Sample PDF (if the issue is file-specific)

---

## Fixed Issues

Issues that have been fixed in recent releases:

| Issue | Fixed In | Description |
|-------|----------|-------------|
| Null pointer crash on corrupted PDFs | v2.0.0 | Added proper error handling |
| Storage full errors not handled | v2.0.0 | Added pre-flight storage checks |
| Bookmark loss on app restart | v2.0.0 | Fixed Room database migration |
| Landscape layout issues | v2.0.0 | Implemented responsive sheets |
| Remember password not auto-filling (#43) | v2.2.0 | Saved password is now auto-submitted on reopen; stale entries cleaned up |
| Double-tap zoom locked to 200% and top-left anchored (#42) | v2.2.0 | Zoom level is configurable (1.1×–5×) and now centers on the tapped point |
| Reader/home chrome ignoring app theme | v2.2.0 | Colors routed through Material color scheme so Dark/Black themes apply |
| PDFs with an apostrophe in the file path not opening (#57) | v2.3.0 | Fixed JS argument escaping so the document loads |
| In-PDF search skipping matches / erratic arrow navigation | v2.3.0 | Search now traverses every match across all pages, with the active match centered |

---

*Last updated: 2026-07-19*
