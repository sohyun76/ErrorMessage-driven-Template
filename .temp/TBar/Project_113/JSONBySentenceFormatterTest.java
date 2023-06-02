package cc.redpen.formatter;

import cc.redpen.RedPen;
import cc.redpen.RedPenException;
import cc.redpen.config.Configuration;
import cc.redpen.config.ValidatorConfiguration;
import cc.redpen.model.Document;
import cc.redpen.model.Sentence;
import cc.redpen.parser.DocumentParser;
import cc.redpen.parser.SentenceExtractor;
import cc.redpen.tokenizer.WhiteSpaceTokenizer;
import cc.redpen.validator.ValidationError;
import cc.redpen.validator.Validator;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class JSONBySentenceFormatterTest extends Validator {
    @Test
    void testFormat() throws JSONException {
        JSONFormatter formatter = new JSONBySentenceFormatter();
        List<ValidationError> errors = new ArrayList<>();
        setErrorList(errors);
        addLocalizedError(new Sentence("testing JSONFormatter", 1));
        Document document = Document.builder().setFileName("docName").build();
        String result = formatter.format(document, errors);

        JSONObject jsonObject = new JSONObject(result);
        String docName = jsonObject.getString("document");
        assertEquals("docName", docName);
        JSONArray jsonErrors = jsonObject.getJSONArray("errors");
        assertNotNull(jsonErrors);
        assertEquals(1, jsonErrors.length());
        JSONObject sentenceErrors = jsonErrors.getJSONObject(0);
        assertEquals("testing JSONFormatter", sentenceErrors.getString("sentence"));
        assertEquals(0, sentenceErrors.getJSONObject("position").getJSONObject("start").getInt("offset"));
        assertEquals(1, sentenceErrors.getJSONObject("position").getJSONObject("start").getInt("line"));
        assertEquals(20, sentenceErrors.getJSONObject("position").getJSONObject("end").getInt("offset"));
        assertEquals(1, sentenceErrors.getJSONObject("position").getJSONObject("end").getInt("line"));
        assertNotNull(sentenceErrors.getJSONArray("errors"));
        assertEquals(1, sentenceErrors.getJSONArray("errors").length());
        JSONObject error = sentenceErrors.getJSONArray("errors").getJSONObject(0);
        assertEquals("json by sentence test error", error.getString("message"));
        assertEquals("JSONBySentenceFormatterTest", error.getString("validator"));
        assertEquals("Error", error.getString("level"));
    }

    @Test
    void testFormatErrorsFromMarkdownParser() throws RedPenException, JSONException {
        String sampleText = "This is a good day。"; // invalid end of sentence symbol
        Configuration conf = Configuration.builder().build();
        Configuration configuration = Configuration.builder()
                .addValidatorConfig(new ValidatorConfiguration("InvalidSymbol"))
                .build();

        List<Document> documents = new ArrayList<>();
        DocumentParser parser = DocumentParser.MARKDOWN;
        documents.add(parser.parse(sampleText,
                new SentenceExtractor(conf.getSymbolTable()), conf.getTokenizer()));
        RedPen redPen = new RedPen(configuration);
        List<ValidationError> errors = redPen.validate(documents).get(documents.get(0));

        JSONFormatter formatter = new JSONBySentenceFormatter();
        String resultString = formatter.format(new cc.redpen.model.Document.DocumentBuilder(
                new WhiteSpaceTokenizer()).build(), errors);
        JSONObject jsonObject = new JSONObject(resultString);
        JSONArray jsonErrors = jsonObject.getJSONArray("errors");
        assertEquals(1, jsonErrors.length());
        JSONObject sentenceErrors = jsonErrors.getJSONObject(0);
        assertEquals("This is a good day。", sentenceErrors.getString("sentence"));
        assertEquals(0, sentenceErrors.getJSONObject("position").getJSONObject("start").getInt("offset"));
        assertEquals(1, sentenceErrors.getJSONObject("position").getJSONObject("start").getInt("line"));
        assertEquals(18, sentenceErrors.getJSONObject("position").getJSONObject("end").getInt("offset"));
        assertEquals(1, sentenceErrors.getJSONObject("position").getJSONObject("end").getInt("line"));
        assertNotNull(sentenceErrors.getJSONArray("errors"));
        assertEquals(1, sentenceErrors.getJSONArray("errors").length());
        JSONObject error = sentenceErrors.getJSONArray("errors").getJSONObject(0);
        assertEquals("InvalidSymbol", error.getString("validator"));
        assertEquals("Error", error.getString("level"));
    }

    @Test
    void testFormatDocumentsSettingErrorLevel() throws RedPenException, JSONException {
        String sampleText = "This is a good day。"; // invalid end of sentence symbol
        Configuration conf = Configuration.builder().build();
        Configuration configuration = Configuration.builder()
                .addValidatorConfig(new ValidatorConfiguration("InvalidSymbol").setLevel(ValidatorConfiguration.LEVEL.INFO))
                .build();

        List<Document> documents = new ArrayList<>();
        DocumentParser parser = DocumentParser.MARKDOWN;
        documents.add(parser.parse(sampleText,
                new SentenceExtractor(conf.getSymbolTable()), conf.getTokenizer()));
        RedPen redPen = new RedPen(configuration);
        List<ValidationError> errors = redPen.validate(documents, "info").get(documents.get(0));

        JSONFormatter formatter = new JSONBySentenceFormatter();
        String resultString = formatter.format(new cc.redpen.model.Document.DocumentBuilder(
                new WhiteSpaceTokenizer()).build(), errors);

        JSONObject jsonObject = new JSONObject(resultString);
        JSONArray jsonErrors = jsonObject.getJSONArray("errors");
        assertEquals(1, jsonErrors.length());
        JSONObject sentenceErrors = jsonErrors.getJSONObject(0);
        assertNotNull(sentenceErrors.getJSONArray("errors"));
        assertEquals(1, sentenceErrors.getJSONArray("errors").length());
        JSONObject error = sentenceErrors.getJSONArray("errors").getJSONObject(0);
        assertEquals("InvalidSymbol", error.getString("validator"));
        assertEquals("Info", error.getString("level"));
    }
}
