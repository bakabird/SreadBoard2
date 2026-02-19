# BOOK MODULE GUIDE

## OVERVIEW

`modules/book` is the format/parsing library. It provides EPUB and UMD readers/writers and related domain models used by the app module.

## STRUCTURE

```text
modules/book/
|- src/main/java/me/ag2s/
|  |- epublib/  # EPUB processing pipeline and domain models
|  |- umdlib/   # UMD parsing/models/tools
|  |- base/     # shared low-level helpers
|- src/main/resources/
|  |- dtd/      # XML/DTD support files
|  |- log4j.properties
```

## WHERE TO LOOK

- EPUB entry points: `modules/book/src/main/java/me/ag2s/epublib/epub/EpubReader.java`, `modules/book/src/main/java/me/ag2s/epublib/epub/EpubWriter.java`
- EPUB metadata/package: `modules/book/src/main/java/me/ag2s/epublib/epub/PackageDocumentReader.java`
- UMD entry points: `modules/book/src/main/java/me/ag2s/umdlib/umd/UmdReader.java`
- UMD models/tools: `modules/book/src/main/java/me/ag2s/umdlib/domain/`, `modules/book/src/main/java/me/ag2s/umdlib/tool/`

## CONVENTIONS

- Keep this module app-agnostic: parsing/model logic only.
- Preserve compatibility in public reader/writer behavior used by app import/read flows.
- Resource-related changes (DTD/package parsing) must be validated against existing EPUB/UMD inputs.

## ANTI-PATTERNS

- Do not introduce Android UI/activity dependencies here.
- Do not remove or casually modify bundled DTD resources.
