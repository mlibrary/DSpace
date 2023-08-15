/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.util;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;
import javax.annotation.Nullable;

import org.apache.commons.lang3.StringUtils;
import org.dspace.content.MetadataSchemaEnum;
import org.dspace.core.Utils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

// UM Change
import javax.servlet.http.HttpServletRequest;
import org.dspace.core.Context;
import javax.servlet.ServletException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


// For UM Changes.
import org.dspace.core.Constants;
import org.dspace.eperson.EPerson;
import java.util.UUID;
import org.dspace.content.service.CollectionService;
import org.dspace.content.factory.ContentServiceFactory;

//UM Changes
import org.dspace.web.ContextUtil;
import org.dspace.services.RequestService;
import org.dspace.services.model.Request;
import org.dspace.utils.DSpace;
import org.dspace.services.factory.DSpaceServicesFactory;
import org.dspace.content.Collection;

/**
 * Class representing a line in an input form.
 *
 * @author Brian S. Hughes, based on work by Jenny Toves, OCLC
 */
public class DCInput {

    private CollectionService collectionService =
        ContentServiceFactory.getInstance().getCollectionService();


    private static final Logger log = LoggerFactory.getLogger(DCInput.class);

    /**
     * the DC element name
     */
    private String dcElement = null;

    /**
     * the DC qualifier, if any
     */
    private String dcQualifier = null;

    /**
     * the DC namespace schema
     */
    private String dcSchema = null;

    /**
     * the input language
     */
    private boolean language = false;

    /**
     * the language code use for the input
     */
    private static final String LanguageName = "common_iso_languages";

    /**
     * the language list and their value
     */
    private List<String> valueLanguageList = null;

    /**
     * a label describing input
     */
    private String label = null;

    /**
     * a style instruction to apply to the input. The exact way to use the style value is UI depending that receive the
     * value from the REST API as is
     */
    private String style = null;

    /**
     * the input type
     */
    private String inputType = null;

    /**
     * is input required?
     */
    private boolean required = false;

    /**
     * if required, text to display when missing
     */
    private String warning = null;

    /**
     * is input repeatable?
     */
    private boolean repeatable = false;

    /**
     * should name-variants be used?
     */
    private boolean nameVariants = false;

    /**
     * 'hint' text to display
     */
    private String hint = null;

    /**
     * if input list-controlled, name of list
     */
    private String valueListName = null;

    /**
     * if input list-controlled, the list itself
     */
    // This is how it was declared in 7.6 originally.
    //private List<String> valueList = null;
    private List<String> valueList = new ArrayList<String>();



    /**
     * if non-null, visibility scope restriction
     */
    private String visibility = null;

    /**
     * if non-null, readonly out of the visibility scope
     */
    private String readOnly = null;

    /**
     * the name of the controlled vocabulary to use
     */
    private String vocabulary = null;

    /**
     * is the entry closed to vocabulary terms?
     */
    private boolean closedVocabulary = false;

    /**
     * the regex in ECMAScript standard format, usable also by rests.
     */
    private String regex = null;

    /**
     * the computed pattern, null if nothing
     */
    private Pattern pattern = null;

    /**
     * allowed document types
     */
    private List<String> typeBind = null;

    private boolean isRelationshipField = false;
    private boolean isMetadataField = false;
    private String relationshipType = null;
    private String searchConfiguration = null;
    private final String filter;
    private final List<String> externalSources;

    /**
     * The scope of the input sets, this restricts hidden metadata fields from
     * view during workflow processing.
     */
    public static final String WORKFLOW_SCOPE = "workflow";

    /**
     * The scope of the input sets, this restricts hidden metadata fields from
     * view by the end user during submission.
     */
    public static final String SUBMISSION_SCOPE = "submit";

    /**
     * Class constructor for creating a DCInput object based on the contents of
     * a HashMap
     *
     * @param fieldMap named field values.
     * @param listMap  value-pairs map, computed from the forms definition XML file
     */
    public DCInput(Map<String, String> fieldMap, Map<String, List<String>> listMap) {

        // UM Change - needed for mapping and proxy depositor logic.
        Context c = ContextUtil.obtainCurrentRequestContext();
        HttpServletRequest request = null;

        RequestService requestService = new DSpace().getRequestService();

        Request currentRequest = requestService.getCurrentRequest();
        if ( currentRequest != null)
        {
          request = currentRequest.getHttpServletRequest();
        }
        // End UM Change



        dcElement = fieldMap.get("dc-element");
        dcQualifier = fieldMap.get("dc-qualifier");

        // Default the schema to dublin core
        dcSchema = fieldMap.get("dc-schema");
        if (dcSchema == null) {
            dcSchema = MetadataSchemaEnum.DC.getName();
        }

        //check if the input have a language tag
        language = Boolean.valueOf(fieldMap.get("language"));
        valueLanguageList = new ArrayList<>();
        if (language) {
            String languageNameTmp = fieldMap.get("value-pairs-name");
            if (StringUtils.isBlank(languageNameTmp)) {
                languageNameTmp = LanguageName;
            }
            valueLanguageList = listMap.get(languageNameTmp);
        }

        String repStr = fieldMap.get("repeatable");
        repeatable = "true".equalsIgnoreCase(repStr)
            || "yes".equalsIgnoreCase(repStr);
        String nameVariantsString = fieldMap.get("name-variants");
        nameVariants = StringUtils.isNotBlank(nameVariantsString) ?
                nameVariantsString.equalsIgnoreCase("true") : false;
        label = fieldMap.get("label");
        inputType = fieldMap.get("input-type");
        // these types are list-controlled
        if ("dropdown".equals(inputType) || "qualdrop_value".equals(inputType)
            || "list".equals(inputType)) {
            valueListName = fieldMap.get("value-pairs-name");

/// As far as I can tell this does nothing.

            if ( valueListName.equals("collection_mappings") )
            {
                try
                {
                        valueList.add ( "In just DC" );
                        valueList.add ( "In just DC" );


                    // Having this here causes perfomace problems in depoist pages loading and traverssing.
                    //HandleService handleService = HandleServiceFactory.getInstance().getHandleService();
                    //AuthorizeService authorizeService = AuthorizeServiceFactory.getInstance().getAuthorizeService();
                    //ContentServiceFactory contentServiceFactory = ContentServiceFactory.getInstance();
                    //List<Collection> collections = contentServiceFactory.getCollectionService().findAll(c);

                    String pr_collection_id = DSpaceServicesFactory.getInstance().getConfigurationService()
                                                 .getProperty("pr.collectionid");
                    List<Collection> collections = collectionService.findAuthorizedOptimized(c, Constants.ADD);
                    for (Collection t : collections) {
                        String handle = t.getHandle();
                        if ( handle != null )
                        {
                            //DSpaceObject coll = handleService.resolveToObject(c, handle);
                            //Collection coll = (Collection) handleService.resolveToObject(c, handle);
                            //if ( authorizeService.authorizeActionBoolean ( c, coll,  Constants.ADD ) )
                            //{
                                String name = t.getName();
                                UUID     id = t.getID();
                                String the_id = id.toString();

                                if ( !the_id.equals(pr_collection_id) )
                                {
                                    log.info("PROX-JustDC:  Adding collections for mapping.");

                                    valueList.add ( name );
                                    valueList.add ( the_id );
                                }
                            //}
                        }
                    }

                    log.info("PROX-JustDC:  DONE Adding collections for mapping.");

                    valueList.add ( "None" );
                    valueList.add ( "-1" );
                }
                catch (Exception e)
                {
                    log.info("PROX-JustDC: ERROR but it may be OK jose, creating collection mapping context is null.");
                    //Do Nothing
                }

            }

            else if ( valueListName.startsWith("depositor"))
            {

                try
                {

                            String collectionHandle = valueListName.substring(10).replace("_", "/");
                            log.info ("PROX-JustDC: this is the coll=" + collectionHandle);

                        valueList.add ( "In just DC" );
                        valueList.add ( "In just DC" );



                   log.info("PROX-JustDC: Creating depositor pick list " + collectionHandle);

                   //Get the eperson
                   EPerson e = c.getCurrentUser();

                UUID userid = e.getID();

                    // Jim asked to remove this option.
                    //valueList.add ( "" );
                    //valueList.add ( "" );

                    EPerson[] Proxies = e.getProxies ( c, userid, collectionHandle );

                    String nameMain = e.getFullName();
                    String emailMain = e.getEmail();


                    String labelMain = nameMain + ", " + emailMain;
                    valueList.add ( labelMain );

                    //valueList.add ( "SELF" );
                    valueList.add ( "SELF" );
                    for (int i = 0; i < Proxies.length; i++)
                    {
                        String name = Proxies[i].getFullName();
                        String email = Proxies[i].getEmail();
                        UUID id = Proxies[i].getID();

                        String label = name + ", " + email;

                        log.info("PROX-JustDC:  Adding proxies." + label + " " + id.toString());

                        valueList.add ( label );
                        valueList.add ( id.toString() );
                    }

                    log.info("PROX-JustDC:  DONE Adding proxies.");

                }
                catch (Exception e)
                {
                    log.info("PROX-JustDC: ERROR but it may be OK jose, creating the depositor picklist for proxies, request is null");
                    //Do Nothing
                }
            }
            else
            {
                valueList = (List) listMap.get(valueListName);
            }





            // The first value is how it came with 7.6.
            // valueList = listMap.get(valueListName);
            //valueList = (List) listMap.get(valueListName);
        }







        hint = fieldMap.get("hint");
        warning = fieldMap.get("required");
        required = warning != null && warning.length() > 0;
        visibility = fieldMap.get("visibility");
        readOnly = fieldMap.get("readonly");
        vocabulary = fieldMap.get("vocabulary");
        this.initRegex(fieldMap.get("regex"));
        String closedVocabularyStr = fieldMap.get("closedVocabulary");
        closedVocabulary = "true".equalsIgnoreCase(closedVocabularyStr)
            || "yes".equalsIgnoreCase(closedVocabularyStr);

        // parsing of the <type-bind> element (using the colon as split separator)
        typeBind = new ArrayList<String>();
        String typeBindDef = fieldMap.get("type-bind");
        if (typeBindDef != null && typeBindDef.trim().length() > 0) {
            String[] types = typeBindDef.split(",");
            for (String type : types) {
                typeBind.add(type.trim());
            }
        }
         style = fieldMap.get("style");
         isRelationshipField = fieldMap.containsKey("relationship-type");
         isMetadataField = fieldMap.containsKey("dc-schema");
         relationshipType = fieldMap.get("relationship-type");
         searchConfiguration = fieldMap.get("search-configuration");
         filter = fieldMap.get("filter");
         externalSources = new ArrayList<>();
         String externalSourcesDef = fieldMap.get("externalsources");
         if (StringUtils.isNotBlank(externalSourcesDef)) {
             String[] sources = StringUtils.split(externalSourcesDef, ",");
             for (String source: sources) {
                 externalSources.add(StringUtils.trim(source));
            }
        }

    }

    protected void initRegex(String regex) {
        this.regex = null;
        this.pattern = null;
        if (regex != null) {
            try {
                Optional.ofNullable(RegexPatternUtils.computePattern(regex))
                    .ifPresent(pattern -> {
                        this.pattern = pattern;
                        this.regex = regex;
                    });
            } catch (PatternSyntaxException e) {
                log.warn("The regex field of input {} with value {} is invalid!", this.label, regex);
            }
        }
    }

    /**
     * Is this DCInput for display in the given scope? The scope should be
     * either "workflow" or "submit", as per the input forms definition. If the
     * internal visibility is set to "null" then this will always return true.
     *
     * @param scope String identifying the scope that this input's visibility
     *              should be tested for
     * @return whether the input should be displayed or not
     */
    public boolean isVisible(String scope) {
        return visibility == null || visibility.equals(scope);
    }

    /**
     * Is this DCInput for display in readonly mode in the given scope?
     * If the scope differ from which in visibility field then we use the out attribute
     * of the visibility element. Possible values are: hidden (default) and readonly.
     * If the DCInput is visible in the scope then this methods must return false
     *
     * @param scope String identifying the scope that this input's readonly visibility
     *              should be tested for
     * @return whether the input should be displayed in a readonly way or fully hidden
     */
    public boolean isReadOnly(String scope) {
        if (isVisible(scope)) {
            return false;
        } else {
            return readOnly != null && readOnly.equalsIgnoreCase("readonly");
        }
    }


    /**
     * Get the repeatable flag for this row
     *
     * @return the repeatable flag
     */
    public boolean isRepeatable() {
        return repeatable;
    }

    /**
     * Alternate way of calling isRepeatable()
     *
     * @return the repeatable flag
     */
    public boolean getRepeatable() {
        return isRepeatable();
    }

    /**
     * Get the nameVariants flag for this row
     *
     * @return the nameVariants flag
     */
    public boolean areNameVariantsAllowed() {
        return nameVariants;
    }

    /**
     * Get the input type for this row
     *
     * @return the input type
     */
    public @Nullable String getInputType() {
        return inputType;
    }

    /**
     * Get the DC element for this form field.
     *
     * @return the DC element
     */
    public String getElement() {
        return dcElement;
    }

    /**
     * Get the DC namespace prefix for this form field.
     *
     * @return the DC namespace prefix
     */
    public String getSchema() {
        return dcSchema;
    }

    /**
     * Get the warning string for a missing required field, formatted for an
     * HTML table.
     *
     * @return the string prompt if required field was ignored
     */
    public String getWarning() {
        return warning;
    }

    /**
     * Is there a required string for this form field?
     *
     * @return true if a required string is set
     */
    public boolean isRequired() {
        return required;
    }

    /**
     * Get the DC qualifier for this form field.
     *
     * @return the DC qualifier
     */
    public String getQualifier() {
        return dcQualifier;
    }

    /**
     * Get the language for this form field.
     *
     * @return the language state
     */
    public boolean getLanguage() {
        return language;
    }

    /**
     * Get the hint for this form field
     *
     * @return the hints
     */
    public String getHints() {
        return hint;
    }

    /**
     * Get the label for this form field.
     *
     * @return the label
     */
    public String getLabel() {
        return label;
    }

    /**
     * Get the style for this form field
     *
     * @return the style
     */
    public String getStyle() {
        return style;
    }

    /**
     * Get the name of the pairs type
     *
     * @return the pairs type name
     */
    public String getPairsType() {
        return valueListName;
    }

    /**
     * Get the name of the pairs type
     *
     * @return the pairs type name
     */
    public List getPairs() {
        return valueList;
    }

    /**
     * Get the list of language tags
     *
     * @return the list of language
     */

    public List<String> getValueLanguageList() {
        return valueLanguageList;
    }

    /**
     * Get the name of the controlled vocabulary that is associated with this
     * field
     *
     * @return the name of associated the vocabulary
     */
    public String getVocabulary() {
        return vocabulary;
    }

    /**
     * Set the name of the controlled vocabulary that is associated with this
     * field
     *
     * @param vocabulary the name of the vocabulary
     */
    public void setVocabulary(String vocabulary) {
        this.vocabulary = vocabulary;
    }

    /**
     * Gets the display string that corresponds to the passed storage string in
     * a particular display-storage pair set.
     *
     * @param pairTypeName Name of display-storage pair set to search
     * @param storedString the string that gets stored
     * @return the displayed string whose selection causes storageString to be
     * stored, null if no match
     */
    public String getDisplayString(String pairTypeName, String storedString) {
        if (valueList != null && storedString != null) {
            for (int i = 0; i < valueList.size(); i += 2) {
                if (storedString.equals(valueList.get(i + 1))) {
                    return valueList.get(i);
                }
            }
        }
        return null;
    }

    /**
     * Gets the stored string that corresponds to the passed display string in a
     * particular display-storage pair set.
     *
     * @param pairTypeName    Name of display-storage pair set to search
     * @param displayedString the string that gets displayed
     * @return the string that gets stored when displayString gets selected,
     * null if no match
     */
    public String getStoredString(String pairTypeName, String displayedString) {
        if (valueList != null && displayedString != null) {
            for (int i = 0; i < valueList.size(); i += 2) {
                if (displayedString.equals(valueList.get(i))) {
                    return valueList.get(i + 1);
                }
            }
        }
        return null;
    }

    /**
     * The closed attribute of the vocabulary tag for this field as set in
     * submission-forms.xml
     *
     * {@code
     * <field>
     * .....
     * <vocabulary closed="true">nsrc</vocabulary>
     * </field>
     * }
     *
     * @return the closedVocabulary flags: true if the entry should be restricted
     * only to vocabulary terms, false otherwise
     */
    public boolean isClosedVocabulary() {
        return closedVocabulary;
    }

    /**
     * Decides if this field is valid for the document type
     *
     * @param typeName Document type name
     * @return true when there is no type restriction or typeName is allowed
     */
    public boolean isAllowedFor(String typeName) {
        return true;
        //if (typeBind.size() == 0) {
        //    return true;
        // }

        //return typeBind.contains(typeName);
    }

    public String getScope() {
        return visibility;
    }

    public Pattern getPattern() {
        return this.pattern;
    }

    public String getRegex() {
        return this.regex;
    }

    public String getFieldName() {
        return Utils.standardize(this.getSchema(), this.getElement(), this.getQualifier(), ".");
    }

    public String getRelationshipType() {
        return relationshipType;
    }

    public String getSearchConfiguration() {
        return searchConfiguration;
    }

    public String getFilter() {
        return filter;
    }

    public List<String> getExternalSources() {
        return externalSources;
    }

    public boolean isQualdropValue() {
        if ("qualdrop_value".equals(getInputType())) {
            return true;
        }
        return false;
    }

    public boolean validate(String value) {
        // if (StringUtils.isNotBlank(value)) {
        //     try {
        //         if (this.pattern != null) {
        //             if (!pattern.matcher(value).matches()) {
        //                 return false;
        //             }
        //         }
        //     } catch (PatternSyntaxException ex) {
        //         log.error("Regex validation failed!  {}", ex.getMessage());
        //     }

        // }
        return true;
    }

    /**
     * Get the type bind list for use in determining whether
     * to display this field in angular dynamic form building
     * @return list of bound types
     */
    public List<String> getTypeBindList() {
        return typeBind;
    }

    /**
     * Verify whether the current field contains an entity relationship.
     * This also implies a relationship type is defined for this field.
     * The field can contain both an entity relationship and a metadata field
     * simultaneously.
     * @return true if the field contains a relationship.
     */
    public boolean isRelationshipField() {
        return isRelationshipField;
    }

    /**
     * Verify whether the current field contains a metadata field.
     * This also implies a field type is defined for this field.
     * The field can contain both an entity relationship and a metadata field
     * simultaneously.
     * @return true if the field contains a metadata field.
     */
    public boolean isMetadataField() {
        return isMetadataField;
    }
}
