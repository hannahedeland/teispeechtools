---
author: Bernhard Fisseni
title: Normalization for TEI XML files
lang: en
---

# Purpose

This documentation describes the tools for processing transcripts of
spoken data in
[TEI](http://www.tei-c.org/release/doc/tei-p5-doc/en/html/TS.html).
Some of the tools can also be applied to TEI documents which are at
least `<w>`-annotated.


# Availability

This is the library containing the tools. A [companion
website](https://clarin.ids-mannheim.de/webstuhl) describes how the
functionality can be accessed as
[REST](https://en.wikipedia.org/wiki/Representational_state_transfer)ful
[web services](https://en.wikipedia.org/wiki/Web_service) where you
can upload documents and download them in the processed form.  The web
services are also available via
[WebLicht](https://weblicht.sfs.uni-tuebingen.de/), but by processing
TEI-encoded files they break with WebLicht's convention of processing
[TCF](https://weblicht.sfs.uni-tuebingen.de/weblichtwiki/index.php/The_TCF_Format)
files.


## Run CLI

All functions are also accessible from the command line.  Try:

(after building in root directory)
```sh
java -cp 'target/dependency/*' -jar target/teispeechtools-0.2-SNAPSHOT.jar
```

(in the directory containing the jar)

```sh
java -cp dependency/*' -jar teispeechtools-0.2-SNAPSHOT.jar
```

and follow the help. Together with this description, you should get
along well.  If not, contact [Bernhard
Fisseni](mailto:fisseni@ids-mannheim.de?subect=TEI+Transcription+tools)


# Tools

## Plain Text to ISO-TEI-annotated texts (CLI command `text2iso`)

Input
: a [plain text](https://en.wikipedia.org/wiki/Plain_text) file
  containing a transcription in the Simple EXMARaLDA format. This
  format permits to encode utterances and overlap between them as well
  as incidents occurring independently or simultaneously to an
  utterance and a commentary (e.g. a translation) on utterances or
  incidents.

Output
: a transcription file conforming to the TEI specification, which is
  already split in annotations: `<annotationBlock>` and `<u>`,
  `<incident>` elements together with `<spanGrp>`s containing the
  commentary. A `<timeline>` is derived from the text, and all
  annotation is situated with respect to the `<timeline>`.

Parameters
: can be specified:

    - the `language` of the utterance.


## Segmentation according to transcription convention (CLI command `segmentize`)

Input
: a TEI-conformant XML document containing `<u>` elements which
  contain plain text formatted according to a transcription convention
  (generic, cGAT minimal, cGAT basic) and potentially `<anchor>`
  elements referring to the `<timeline>`.  This can be the output of
  the previously described plain text conversion.
  
Output
: a TEI-conformant XML document in which the `<u>` elements have been
  segmented into words `<w>` on the one hand and conventions have been
  resolved to XML markup like `<pause>`, `<gap>` etc.
  
Parameters
: You can specify:

    - the `language` of the document (if there is language information
      in the document, it will be preferred),
    - the transcription convention which the text contents of the
      `<u>` follows. Currently `generic`, (cGAT) `minimal` and (cGAT)
      `basic` are supported.


## Language-detection (CLI command `guess`)

Input
: a TEI-conformant XML document containing `<u>` elements which
  contain either plain text and `<anchor>` elements only or have been
  analysed into `<w>` (other contents possible).  In the first case,
  the whole text content (excluding) will be processed; in the latter
  case, only the contents of `<w>` elements will be processed.
  
Output
: a TEI-conformant XML document where the `<u>` have been annoted with
  `@xml:lang` attributes where the algorithm reached a decision. Cases
  of doubt are reported in XML comments.  If languages are equally
  probable, the document languare is preferred.

Parameters
: You can specify:

    - the `language` of the document (if there is language information
      in the document, it will be preferred),


## OrthoNormal-like Normalization (command `normalize`)

Input
: a TEI-conformant XML document containing `<u>` elements which have
  been analysed into `<w>` (other contents possible).

Output
: a TEI-conformant XML document where the `<w>` have been annoted with
  a `@norm` attribute containing the normalized form.  Currently,
  normalization is only applied to text in German.

Parameters
: You can specify:

    - the `language` of the document (if there is language information
      in the document, it will be preferred).


This service is based on the algorithm in [OrthoNormal (German
description
only)](http://exmaralda.org/de/orthonormal-de/). [`DictionaryNormalizer`](target/apidocs/de/ids/mannheim/clarin/teispeech/tools/class-use/DictionaryNormalizer.html),
which applies normalization based on dictionaries from the
[FOLK](http://agd.ids-mannheim.de/folk.shtml) and the
[DeReKo](http://www1.ids-mannheim.de/kl/projekte/korpora.html)
corpora.

It basically searches for `<w>` elements and applies normalization to
their [text
content](https://www.w3schools.com/xml/prop_element_textcontent.asp).


Normalization:

- Step 1: The most frequent normalization for a word form in the
  [FOLK](http://agd.ids-mannheim.de/folk.shtml) corpus is applied.
- Step 2: If nothing is found in Step 1, the list of words that occur
  capitalized-only in the [Deutsches Referenzkorpus
  (DeReKo)](http://www1.ids-mannheim.de/kl/projekte/korpora.html)
  is consulted and a normalization is chosen.
- Step 3: Out-of-dictionary words are left as is.


## POS-Tagging with the TreeTagger

Input
: a TEI-conformant XML document containing `<u>` elements which have
  been analysed into `<w>` (other contents possible).

Output
: a TEI-conformant XML document where the `<w>` have been pos-tagged
  (`@pos` attribute) and lemmatized (`@lemma` attribute) with the
  [TreeTagger](http://www.cis.uni-muenchen.de/~schmid/tools/TreeTagger/).

Parameters
: You can specify:

    - the `language` of the document (if there is language information
      in the document, it will be preferred).


# Building and inspecting

## Compilation

```sh
    mvn install dependency:copy-dependencies
```

installs the package locally with Maven and copies the dependencies to
`target/dependency`.  You can use this package as a library then, or
use the CLI (see below).  If you only want to use the CLI, you can run:

```sh
    mvn package dependency:copy-dependencies
```

This will pull in the sources and make a runnable [JAR](https://en.wikipedia.org/wiki/JAR_%28file_format%29)



## Compile Dictionary

To speed up loading time, one can compile the dictionary, which
combines the FOLK and DeReKo dictionaries described above.  The
combined dictionary is contained in downloads.  From the root
directory of the project, execute:

```sh
java -cp 'target/teispeechtools-0.1-SNAPSHOT.jar:target/dependency/*' \
    de.ids.mannheim.clarin.teispeech.tools.DictMaker
```


## Check Pattern files

To check the files with regular expressions for transcription
conventions by running:

```sh
java -cp 'target/dependency/*:target/teispeechtools-0.1-SNAPSHOT.jar' \
    de.ids.mannheim.clarin.teispeech.tools.PatternReaderRunner -h
```

to get help. E.g.,

```sh
java -cp 'target/dependency/*:target/teispeechtools-0.2-SNAPSHOT.jar' \
    de.ids.mannheim.clarin.teispeech.tools.PatternReaderRunner \
    -i src/main/xml/Patterns.xml --language universal --level 2
```

(only levels 2 and 3 are available)
