/**
 * redpen: a text inspection tool
 * Copyright (c) 2014-2015 Recruit Technologies Co., Ltd. and contributors
 * (see CONTRIBUTORS.md)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package cc.redpen.server.api;

import cc.redpen.RedPen;
import cc.redpen.RedPenException;
import cc.redpen.config.ConfigurationExporter;
import cc.redpen.config.Symbol;
import cc.redpen.config.SymbolType;
import cc.redpen.parser.DocumentParser;
import cc.redpen.validator.Validator;
import org.apache.wink.common.annotations.Workspace;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.ServletContext;
import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Map;

/**
 * Resource to get and set RedPen configuration options.
 */
@Workspace(workspaceTitle = "RedPen", collectionTitle = "Configuration")
@Path("/config")
public class RedPenConfigurationResource {

    private static final Logger LOG = LoggerFactory.getLogger(RedPenConfigurationResource.class);

    @Context
    private ServletContext context;

    RedPenService getRedPenService() throws RedPenException {
        return new RedPenService(context);
    }

    @Path("/redpens")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @WinkAPIDescriber.Description("Return the configuration for available redpens matching the supplied language (default is any language)")
    public Response getRedPens(@QueryParam("lang") @DefaultValue("") String lang) throws RedPenException {

        JSONObject response = new JSONObject();

        // add the known document formats
        try {
            response.put("version", RedPen.VERSION);
            response.put("documentParsers", DocumentParser.PARSER_MAP.keySet());

            // add matching configurations
            Map<String, RedPen> redpens = getRedPenService().getRedPens();
            final JSONObject redpensJSON = new JSONObject();
            response.put("redpens", redpensJSON);
            redpens.forEach((configurationName, redPen) -> {
                if ((lang == null) || lang.isEmpty() || redPen.getConfiguration().getLang().contains(lang)) {
                    try {
                        // add specific configuration items
                        JSONObject config = new JSONObject();
                        config.put("lang", redPen.getConfiguration().getLang());
                        config.put("variant", redPen.getConfiguration().getVariant());
                        config.put("tokenizer", redPen.getConfiguration().getTokenizer().getClass().getName());

                        // add the names of the validators
                        JSONObject validatorConfigs = new JSONObject();
                        for (Validator validator : redPen.getValidators()) {
                            JSONObject validatorJSON = new JSONObject();
                            String name = validator.getClass().getSimpleName().endsWith("Validator")
                              ? validator.getClass().getSimpleName().substring(0, validator.getClass().getSimpleName().length() - 9)
                              : validator.getClass().getSimpleName();
                            validatorJSON.put("languages", validator.getSupportedLanguages());
                            validatorJSON.put("properties", validator.getConfigAttributes());
                            validatorConfigs.put(name, validatorJSON);
                        }
                        config.put("validators", validatorConfigs);

                        // add the symbol table
                        JSONObject symbolConfigs = new JSONObject();
                        for (SymbolType symbolType : redPen.getConfiguration().getSymbolTable().getNames()) {
                            JSONObject symbolJSON = new JSONObject();
                            Symbol symbol = redPen.getConfiguration().getSymbolTable().getSymbol(symbolType);
                            symbolJSON.put("value", String.valueOf(symbol.getValue()));
                            symbolJSON.put("invalid_chars", String.valueOf(symbol.getInvalidChars()));
                            symbolJSON.put("after_space", symbol.isNeedAfterSpace());
                            symbolJSON.put("before_space", symbol.isNeedBeforeSpace());
                            symbolConfigs.put(symbolType.toString(), symbolJSON);
                        }
                        config.put("symbols", symbolConfigs);

                        redpensJSON.put(configurationName, config);
                    }
                    catch (Exception e) {
                        LOG.error("Exception when rendering RedPen to JSON for configuration " + configurationName, e);
                    }
                }
            });
        }
        catch (Exception e) {
            LOG.error("Exception when rendering RedPen to JSON", e);
        }

        return Response.ok().entity(response).build();
    }

    @Path("/export")
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_XML)
    @WinkAPIDescriber.Description("Returns the configuration XML corresponds to the UI")
    public Response exportConfiguration(JSONObject requestJSON) throws RedPenException {
        LOG.info("Exporting configuration using JSON request");
        RedPen redPen = new RedPenService(context).getRedPenFromJSON(requestJSON);
        String result = null;
        try(ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            new ConfigurationExporter().export(redPen.getConfiguration(), baos);
            result = new String(baos.toByteArray());
        } catch (IOException e) {
            LOG.error("Exception when exporting configuration", e);
        }
        return Response.ok(result, RedPenResource.MIME_TYPE_XML).build();
    }
}
