package de.ids.mannheim.clarin.teispeech.tools;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.stream.Stream;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.CharStreams;
import org.jdom2.JDOMException;
import org.jdom2.output.Format;
import org.jdom2.output.XMLOutputter;
import org.korpora.useful.LangUtilities;
import org.korpora.useful.Utilities;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import de.ids.mannheim.clarin.teispeech.data.GATParser;
import de.ids.mannheim.clarin.teispeech.data.NameSpaces;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Model.CommandSpec;
import picocli.CommandLine.Option;
import picocli.CommandLine.ParameterException;
import picocli.CommandLine.Parameters;
import picocli.CommandLine.Spec;

/**
 * a command line interface for the annotation processing work flow
 *
 * @author bfi
 *
 */
@Command(description = "process documents of speech annotated "
        + "according to TEI/ISO", sortOptions = false, name = "spindel"
                + "", mixinStandardHelpOptions = true, versionProvider = VersionProvider.class)
public class CLI implements Runnable {

    // @Option(names = {"-v", "--verbose"}, description = "give more info")
    // private boolean verbose = false;
    //
    @Option(names = { "--indent" }, description = "indent")
    private boolean indent = false;
    // @Command() static void normalize

    @Option(names = { "--force" }, description = "force STEP even if "
            + "the corresponding annotation exists already "
            + "(NOT for text2iso, segmentize)")
    private boolean force = false;
    // @Command() static void normalize

    private enum Step {
        text2iso, segmentize, guess, normalize, pos, identify, unidentify,
        align;
    };

    @Parameters(index = "0", paramLabel = "STEP", description = "Processing "
            + "Step, one of: ${COMPLETION-CANDIDATES}")
    private Step step;

    @Option(names = { "-i",
            "--input" }, description = "input file , by default STDIN")
    private File inputFile;

    @Option(names = { "-o",
            "--output" }, description = "output file, by default STDOUT")
    private File outFile;

    @Option(names = { "-l",
            "--language" }, description = "the (default) language "
                    + "of the document, an ISO-639 language code "
                    + "(default: '${DEFAULT-VALUE}'; normalize, pos)")
    private String language = "deu";

    @Option(names = "--expected", description = "comma-separated "
            + "list of expected languages besides the main language; "
            + "by default '${DEFAULT-VALUE}' "
            + "(ONLY guess)", defaultValue = "deu,eng,tur", split = ",")
    String[] expected;

    @Option(names = { "-k",
            "--keep-case" }, description = "do not convert to lower case "
                    + "when normalizing; effectively, skip capitalized words")
    boolean keepCase = false;

    @Option(names = { "-L",
            "--level" }, description = "the level of the transcription "
                    + "(segmentize, default: '${DEFAULT-VALUE}')")
    private ProcessingLevel level = ProcessingLevel.generic;

    @Option(names = {
            "--minimal" }, description = "the `minimal count` of words so "
                    + "that language detection is even "
                    + "tried (default: ${DEFAULT-VALUE}, "
                    + "which is already pretty low)")
    private int minimalLength = 5;

    @Option(names = { "-p",
            "--use-phones" }, description = "use (pseudo)phones "
                    + "for pseudoalignment (default: ${DEFAULT-VALUE})")
    boolean usePhones = true;
    @Option(names = { "-t",
            "--transcribe" }, description = "add phonetic canonical transcription"
                    + "in pseudoalignment (default: ${DEFAULT-VALUE}, only used "
                    + "if --use-phones)")
    boolean transcribe = true;

    @Spec
    private CommandSpec spec; // injected by picocli

    private OutputStream outStream = System.out;

    private InputStream inputStream = System.in;

    private static DocumentBuilderFactory factory = DocumentBuilderFactory
            .newInstance();
    private static DocumentBuilder builder;
    static {
        try {
            factory.setNamespaceAware(true);
            builder = factory.newDocumentBuilder();
        } catch (ParserConfigurationException e) {
            throw new RuntimeException(e);
        }
    }

    public static void main(String[] args) {
        CommandLine.run(new CLI(), args);
    }

    @Override
    public void run() {
//        System.err.println(String.format("STEP is %s with %s and language %s",
//                step, inputFile, language));
        if (outFile != null) {
            try {
                outStream = new FileOutputStream(outFile);
            } catch (FileNotFoundException e) {
                System.err.println(e.getMessage());
                System.err.println("--> continuing to print to STDOUT");
            }
        }
        if (inputFile != null) {
            try {
                inputStream = new FileInputStream(inputFile);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        language = checkLanguage(language);
        expected = Stream.of(expected).map(this::checkLanguage)
                .toArray(String[]::new);
        switch (step) {
        case text2iso:
            text2iso();
            break;
        case segmentize:
            segmentize();
            break;
        case normalize:
            normalize();
            break;
        case pos:
            pos();
            break;
        case guess:
            guess();
            break;
        case identify:
            identify();
            break;
        case unidentify:
            unidentify();
            break;
        case align:
            pseudoAlign();
            break;
        }
    }

    /**
     * check language parameters
     *
     * @param lang
     *            a String given as a language
     * @return
     */
    private String checkLanguage(String lang) {
        if (!LangUtilities.isLanguage(lang)) {
            throw new ParameterException(spec.commandLine(),
                    String.format("«%s» is not a valid language!", lang));
        } else {
            lang = LangUtilities.getLanguage(lang).get();
        }
        return lang;
    }

    /**
     * convert to ISO
     */
    public void text2iso() {
        CharStream inputCS;
        try {
            inputCS = CharStreams.fromStream(inputStream);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        Document doc = TextToTEIConversion.process(inputCS, language);
        Utilities.outputXML(outStream, doc, indent);

    }

    /**
     * pos-tag an ISO transcription
     */
    public void pos() {
        try {
            Document doc = builder.parse(inputStream);
            TEIPOS teipo = new TEIPOS(doc, language);
            teipo.posTag(force);
            Utilities.outputXML(outStream, doc, indent);
        } catch (IOException | SAXException e) {
            throw new RuntimeException(e);
        }

    }

    /**
     * normalize an ISO transcription
     */
    public void normalize() {
        WordNormalizer wn = new DictionaryNormalizer(keepCase, true);
        TEINormalizer tn = new TEINormalizer(wn, language);
        try {
            Document doc = builder.parse(inputStream);
            System.err.format("Have got %d <w> nodes.\n",
                    doc.getElementsByTagNameNS(NameSpaces.TEI_NS, "w")
                            .getLength());
            tn.normalize(doc, force);
            Utilities.outputXML(outStream, doc, indent);
        } catch (IOException | SAXException e) {
            throw new RuntimeException(e);
        }

    }

    public void normalise() {
        normalize();
    }

    /**
     * guess languages in an ISO transcription
     */
    public void guess() {
        try {
            Document doc = builder.parse(inputStream);
            LanguageDetect ld = new LanguageDetect(doc, language, expected,
                    minimalLength);
            ld.detect(force);
            Utilities.outputXML(outStream, doc, indent);
        } catch (IOException | SAXException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * segment an ISO transcription
     */
    public void segmentize() {
        if (level == ProcessingLevel.generic) {
            try {
                Document doc = builder.parse(inputStream);
                GenericParsing.process(doc);
                Utilities.outputXML(outStream, doc, indent);
            } catch (SAXException | IOException e) {
                throw new RuntimeException(e);
            }

        } else {
            try {
                org.jdom2.Document doc = Utilities.parseXMLviaJDOM(inputStream);
                GATParser parser = new GATParser();
                parser.parseDocument(doc, level.ordinal() + 1);
                DocUtilities.makeChange(doc, String.format(
                        "utterances parsed to %s conventions", level.name()));

                XMLOutputter outputter = new XMLOutputter();
                if (indent) {
                    Format outFormat = Format.getPrettyFormat();
                    outputter.setFormat(outFormat);
                }
                outputter.output(doc, outStream);
            } catch (IOException e) {
                throw new RuntimeException(e);
            } catch (JDOMException e) {
                throw new RuntimeException(e);
            }
        }
    }

    /**
     * add XML IDs
     */
    public void pseudoAlign() {
        try {
            Document doc = builder.parse(inputStream);
            PseudoAlign aligner = new PseudoAlign(doc, language, usePhones,
                    transcribe, force);
            aligner.calculateUtterances();
            Utilities.outputXML(outStream, doc, indent);
        } catch (IOException | SAXException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * add XML IDs
     */
    public void identify() {
        try {
            Document doc = builder.parse(inputStream);
            DocumentIdentifier.makeIDs(doc);
            Utilities.outputXML(outStream, doc, indent);
        } catch (IOException | SAXException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * add XML IDs
     */
    public void unidentify() {
        try {
            Document doc = builder.parse(inputStream);
            DocumentIdentifier.removeIDs(doc);
            Utilities.outputXML(outStream, doc, indent);
        } catch (IOException | SAXException e) {
            throw new RuntimeException(e);
        }
    }

}
