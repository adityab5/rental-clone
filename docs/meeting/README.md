# Module PDF Handouts

This folder contains meeting-ready PDF handouts for backend modules:

- admin
- rental
- payment
- review
- notification

## Generate / Refresh PDFs

Run from project root:

```powershell
powershell -ExecutionPolicy Bypass -File .\scripts\generate-module-pdfs.ps1
```

Generated files are written to:

- `docs/meeting/html/*.html`
- `docs/meeting/pdf/*.pdf`

## Notes

- Content is based on current controller/service implementation.
- PDF rendering uses Microsoft Edge headless mode.

