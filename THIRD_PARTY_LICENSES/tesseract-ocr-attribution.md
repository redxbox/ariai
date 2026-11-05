# Offline OCR notices

AriAI bundles the following open-source components for Persian/English on-device PDF OCR:

- **Tesseract4Android 4.9.0** by Adaptech s.r.o. and contributors, distributed via JitPack (<https://github.com/adaptech-cz/Tesseract4Android>): Apache License 2.0.
- **Tesseract OCR 5.5.1**: Apache License 2.0.
- **Tesseract `eng` and `fas` trained data** from `tesseract-ocr/tessdata_fast` (<https://github.com/tesseract-ocr/tessdata_fast>): Apache License 2.0. Bundled blob IDs: `eng` `bbef4675053b5b468cdb477053e28b1c698ba08e`; `fas` `1a17ee02076107e73abd15d2038389150c8f824f`.
- **Leptonica 1.85.0**: permissive BSD-style license; notice in [`leptonica-license.txt`](leptonica-license.txt).
- **libjpeg 9f** from the Independent JPEG Group: redistribution notice and required acknowledgement in [`ijg-jpeg-notice.txt`](ijg-jpeg-notice.txt).
- **libpng 1.6.48**: PNG Reference Library License; notice in [`libpng-license.txt`](libpng-license.txt).

The full Apache License 2.0 text is in [`apache-2.0.txt`](apache-2.0.txt). The trained-data files are installed into the app's private files directory on first OCR use; documents and extracted text are not sent to an OCR service.
