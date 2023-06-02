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
package cc.redpen.validator;

import cc.redpen.config.ValidatorConfiguration;
import cc.redpen.model.Sentence;
import cc.redpen.parser.LineOffset;

import java.io.Serializable;
import java.util.Optional;

/**
 * Error to report invalid point from Validators.
 */
public class ValidationError implements Serializable {

    private static final long serialVersionUID = -1273191135155157144L;
    private final String message;
    private final String validatorName;
    private final Sentence sentence;
    private final LineOffset startPosition;
    private final LineOffset endPosition;
    private final ValidatorConfiguration.LEVEL level;

    /**
     * Constructor.
     *
     * @param validatorName    validator name
     * @param errorMessage      error message
     * @param sentenceWithError sentence containing validation error
     */
    public ValidationError(String validatorName,
            String errorMessage,
            Sentence sentenceWithError) {
        this(validatorName, errorMessage, sentenceWithError, ValidatorConfiguration.LEVEL.ERROR);
    }

    /**
     * Constructor.
     *
     * @param validatorName    validator name
     * @param errorMessage      error message
     * @param sentenceWithError sentence containing validation error
     */
    public ValidationError(String validatorName,
            String errorMessage,
            Sentence sentenceWithError,
            ValidatorConfiguration.LEVEL level) {
        this.message = errorMessage;
        this.validatorName = validatorName;;
        this.sentence = sentenceWithError;
        this.startPosition = null;
        this.endPosition = null;
        this.level = level;
    }

    /**
     * Constructor.
     *
     * @param validatorName    validator name
     * @param errorMessage      error message
     * @param sentenceWithError sentence containing validation error
     * @param startPosition     position where error starts
     * @param endPosition       position where error ends
     */
    ValidationError(String validatorName, String errorMessage, Sentence sentenceWithError,
            int startPosition, int endPosition) {
        this(validatorName, errorMessage, sentenceWithError, startPosition, endPosition, ValidatorConfiguration.LEVEL.ERROR);
    }

    /**
     * Constructor.
     *
     * @param validatorName    validator name
     * @param errorMessage      error message
     * @param sentenceWithError sentence containing validation error
     * @param startPosition     position where error starts
     * @param endPosition       position where error ends
     */
    ValidationError(String validatorName, String errorMessage, Sentence sentenceWithError,
            int startPosition, int endPosition,
            ValidatorConfiguration.LEVEL level) {
        this.message = errorMessage;
        this.validatorName = validatorName;
        this.sentence = sentenceWithError;
        this.startPosition = sentenceWithError.getOffset(startPosition).get();
        this.endPosition = sentenceWithError.getOffset(endPosition).get();
        this.level = level;
    }

    /**
     * Constructor.
     *
     * @param validatorClass    validator class
     * @param errorMessage      error message
     * @param sentenceWithError sentence containing validation error
     * @param startPosition     position where error starts
     * @param endPosition       position where error ends
     * @deprecated
     */
    ValidationError(Class validatorClass, String errorMessage, Sentence sentenceWithError,
            LineOffset startPosition, LineOffset endPosition) {
        this.message = errorMessage;
        this.validatorName = validatorClass.getSimpleName();
        this.sentence = sentenceWithError;
        this.startPosition = startPosition;
        this.endPosition = endPosition;
        this.level = ValidatorConfiguration.LEVEL.ERROR;
    }

    /**
     * Get line number in which the error occurs.
     *
     * @return the number of line
     */
    public int getLineNumber() {
        return sentence.getLineNumber();
    }

    /**
     * Get error message.
     *
     * @return error message
     */
    public String getMessage() {
        return message;
    }

    /**
     * Get column number in which the error occurs.
     *
     * @return column (character) position which sentence starts
     */
    public int getStartColumnNumber() {
        return sentence.getStartPositionOffset();
    }

    /**
     * Get sentence containing the error.
     *
     * @return sentence
     */
    public Sentence getSentence() {
        return sentence;
    }

    /**
     * Get validator name.
     *
     * @return validator name
     */
    public String getValidatorName() {
        if (validatorName.endsWith("Validator")) {
            return validatorName
                    .substring(0, validatorName.length() - "Validator".length());
        } else {
            return validatorName;
        }
    }

    /**
     * Get error start position.
     *
     * @return error start position (Note: some validation error does not specify the error position)
     */
    public Optional<LineOffset> getStartPosition() {
        return Optional.ofNullable(startPosition);
    }

    /**
     * Get error end position.
     *
     * @return error end position (Note: some validation error does not specify the error position)
     */
    public Optional<LineOffset> getEndPosition() {
        return Optional.ofNullable(endPosition);
    }

    /**
     * Get error level.
     * @return error level
     */
    public ValidatorConfiguration.LEVEL getLevel() {
        return level;
    }

    @Override
    public String toString() {
        return "ValidationError{" +
                "message='" + message + '\'' +
                ", validatorName='" + validatorName + '\'' +
                ", sentence=" + sentence +
                ", startPosition=" + startPosition +
                ", endPosition=" + endPosition +
                ", level=" + level +
                '}';
    }
}
