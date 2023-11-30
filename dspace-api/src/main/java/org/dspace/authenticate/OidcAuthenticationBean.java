/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.authenticate;


import static java.lang.String.format;
import static java.net.URLEncoder.encode;
import static org.apache.commons.lang.BooleanUtils.toBoolean;
import static org.apache.commons.lang3.StringUtils.isAnyBlank;
import static org.apache.commons.lang3.StringUtils.isBlank;

import java.io.UnsupportedEncodingException;
import java.sql.SQLException;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang3.StringUtils;
import org.dspace.authenticate.oidc.OidcClient;
import org.dspace.authenticate.oidc.model.OidcTokenResponseDTO;
import org.dspace.core.Context;
import org.dspace.eperson.EPerson;
import org.dspace.eperson.Group;
import org.dspace.eperson.service.EPersonService;
import org.dspace.services.ConfigurationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

// UM Changes
import java.util.UUID;
import org.dspace.eperson.service.GroupService;
import java.io.*;
import java.net.*;
import org.dspace.services.factory.DSpaceServicesFactory;
import org.dspace.eperson.factory.EPersonServiceFactory;
import java.util.ArrayList;
import java.util.Collections;


/**
 * OpenID Connect Authentication for DSpace.
 *
 * This implementation doesn't allow/needs to register user, which may be holder
 * by the openID authentication server.
 *
 * @link   https://openid.net/developers/specs/
 *
 * @author Luca Giamminonni (luca.giamminonni at 4science.it)
 */
public class OidcAuthenticationBean implements AuthenticationMethod {

    public static final String OIDC_AUTH_ATTRIBUTE = "oidc";

    private final static String LOGIN_PAGE_URL_FORMAT = "%s?client_id=%s&response_type=code&scope=%s&redirect_uri=%s";

    private static final Logger LOGGER = LoggerFactory.getLogger(OidcAuthenticationBean.class);

    private static final String OIDC_AUTHENTICATED = "oidc.authenticated";

    @Autowired
    private ConfigurationService configurationService;

    @Autowired
    private OidcClient oidcClient;

    @Autowired
    private EPersonService ePersonService;

    @Override
    public boolean allowSetPassword(Context context, HttpServletRequest request, String username) throws SQLException {
        return false;
    }

    @Override
    public boolean isImplicit() {
        return false;
    }

    @Override
    public boolean canSelfRegister(Context context, HttpServletRequest request, String username) throws SQLException {
        return canSelfRegister();
    }

    @Override
    public void initEPerson(Context context, HttpServletRequest request, EPerson eperson) throws SQLException {
    }

    @Override
    public List<Group> getSpecialGroups(Context context, HttpServletRequest request) throws SQLException {
      try
            {
                LOGGER.info ("OIDC: start getSpecialGroups");
                String defaultUUID = "00000000-0000-1000-a000-000000000000";
                UUID bioId = UUID.fromString(defaultUUID);
                UUID umId = UUID.fromString(defaultUUID);
                UUID bentId = UUID.fromString(defaultUUID);
                UUID bentOnlyId = UUID.fromString(defaultUUID);
                UUID rcId = UUID.fromString(defaultUUID);
                int count = 0;

                GroupService groupService = EPersonServiceFactory.getInstance().getGroupService();

                //Using this one for testing in docker
                String addr = request.getRemoteAddr();

                // This is the one you should use on the live area.
                //String addr = request.getHeader("X-Forwarded-For");

                LOGGER.info ("OIDC: checking the addr = " + addr);
                //addr = null;

                if ( addr == null )
                {
                    LOGGER.error("OICD:  returning and empty list because address is null");
                    return Collections.emptyList();

                }

                if ( isBioUser( request ) )
                    {

                        LOGGER.error("OICD:  it's in BIO USER");
                        Group bioGroup = groupService.findByName(context, "Bio Users");
                        //Group bioGroup = Group.findByName(context, "Bio Users");
                        // Append to list of elligible groups
                        bioId = bioGroup.getID();
                        count++;

                        LOGGER.info ("OIDC: In: Bio Users " + bioId.toString());
                    }
                else
                    {
                        //Group notbioGroup = Group.findByName(context, "NotBio");
                        Group notbioGroup = groupService.findByName(context, "NotBio");
                        // Append to list of elligible groups
                        bioId = notbioGroup.getID();
                        count++;

                        LOGGER.info ("OIDC: In: NotBio " + bioId.toString());

                    }

                // Put everyone in the Request Copy Group
                //Group rcGroup = Group.findByName(context, "RequestCopy Users");
                Group rcGroup = groupService.findByName(context, "RequestCopy Users");
                // Append to list of elligible groups
                rcId = rcGroup.getID();
                LOGGER.info ("OIDC: In: RequestCopy Users " + rcId.toString());
                count++;

                if ( isBentleyUser( context, request ) )
                    {
                        //Group bentGroup = Group.findByName(context, "Bentley Users");
                        Group bentGroup = groupService.findByName(context, "Bentley Users");
                        // Append to list of elligible groups
                        bentId = bentGroup.getID();
                        count++;
                        LOGGER.info ("OIDC: In: Bentley Users " + bentId.toString());
                    }

                if ( isBentleyOnlyUser( context, request ) )
                    {
                        //Group bentOnlyGroup = Group.findByName(context, "Bentley Only Users");
                        Group bentOnlyGroup = groupService.findByName(context, "Bentley Only Users");
                        // Append to list of elligible groups
                        bentOnlyId = bentOnlyGroup.getID();
                        count++;

                        LOGGER.info("OIDC: In: Bentley Only Users " + bentOnlyId.toString());
                    }

                // If logged in and has access.
                // OR is at a UM Address
                if (hasUMPriviledges(context) || isUMUser(request))
                    {

                        // add the user to the special group "UM Users"
                        //Group umGroup = Group.findByName(context, "UM Users");
                        Group umGroup = groupService.findByName(context, "UM Users");
                        // Append to list of elligible groups
                        umId = umGroup.getID();
                        count++;
                        LOGGER.info("OIDC: In: UM User " + umId.toString());

                    }

                //if ( (bioId == -1) && (umId == -1) && (bentId == -1) && (bentOnlyId == -1) && (rcId == -1) )
                if ( (bioId.compareTo(UUID.fromString(defaultUUID))==1) && (umId.compareTo(UUID.fromString(defaultUUID))==1) && (bentId.compareTo(UUID.fromString(defaultUUID))==1) && (bentOnlyId.compareTo(UUID.fromString(defaultUUID))==1) && (rcId.compareTo(UUID.fromString(defaultUUID))==1) )
                    {

                        LOGGER.info("OIDC: Missing Groups.  Admin needs to create them: Bio, Um, Bent Only, Request Copy");

                        //return ListUtils.EMPTY_LIST;
                        return Collections.emptyList();
                    }

                UUID[] groupIds = new UUID[count];
                int newcount = 0;
                if ( !bioId.equals(UUID.fromString(defaultUUID)) )
                    {
                        groupIds[newcount] = bioId;
                        newcount++;
                    }
                if ( !bentId.equals(UUID.fromString(defaultUUID)) )
                    {
                        groupIds[newcount] = bentId;
                        newcount++;
                    }
                if ( !bentOnlyId.equals(UUID.fromString(defaultUUID)) )
                    {
                        groupIds[newcount] = bentOnlyId;
                        newcount++;
                    }
                if ( !rcId.equals(UUID.fromString(defaultUUID)) )
                    {
                        groupIds[newcount] = rcId;
                        newcount++;
                    }
                if ( !umId.equals(UUID.fromString(defaultUUID)) )
                    {
                        groupIds[newcount] = umId;
                    }

                List<Group> specialGroups = new ArrayList<Group>();
                for(int i = 0; i < groupIds.length; i++)
                {
                        LOGGER.info("OIDC: Group Found and returning " + groupIds[i].toString());

                        Group g =  EPersonServiceFactory.getInstance().getGroupService().find(context, groupIds[i]);;
                        specialGroups.add ( g );
                }

                LOGGER.info("OIDC: Returning all the special groups");
                return specialGroups;

            }
        catch(SQLException sqle)
            {
                LOGGER.info("OIDC: SQL Exception Error.  Returning empty list of groups");
                //return ListUtils.EMPTY_LIST;
                return Collections.emptyList();
                //throw new JspException(ie);
                //              throw new IOException(sqle.getMessage());
            }

    }


    public static boolean isUMUser(HttpServletRequest request)
    {
       //String addr = request.getRemoteAddr();
       String addr = request.getHeader("X-Forwarded-For");

       String ips = DSpaceServicesFactory.getInstance().getConfigurationService()
                                                 .getProperty("ip.umIPs");
       LOGGER.info ("JOSEAUTHips: the ips from config = " + ips);
       final String[] umIPs = ips.split("\\|");

       for (int i = 0; i < umIPs.length; i++)
       {
            LOGGER.info ("JOSEAUTH: umIPs= " + umIPs[i]);
       }

        if ( addr == null )
        {
            return false;
        }

        for (int i = 0; i < umIPs.length; i++)
        {
            if (addr.startsWith(umIPs[i]))
            {
                return true;
            }
        }

        return false;
    }

     public static boolean isBioUser(HttpServletRequest request)
    {
        //String addr = request.getRemoteAddr();
        String addr = request.getHeader("X-Forwarded-For");

        LOGGER.info ( "OIDC:  in isBioUser The addr is (IP) value " + addr );

       String ips = DSpaceServicesFactory.getInstance().getConfigurationService()
                                                 .getProperty("ip.bioIPs");
       LOGGER.info ("JOSEAUTHips: the ips from config = " + ips);
       final String[] bioIPs = ips.split("\\|");

       for (int i = 0; i < bioIPs.length; i++)
       {
            LOGGER.info ("JOSEAUTH: bioIPs = " + bioIPs[i]);
       }

        if ( addr == null )
        {
            return false;
        }

        for (int i = 0; i < bioIPs.length; i++)
        {
            if (addr.startsWith(bioIPs[i]))
            {
                return true;
            }
        }

        return false;
    }
    
   public static boolean isBentleyUser(Context context, HttpServletRequest request)
    {
        //String addr = request.getRemoteAddr();
        String addr = request.getHeader("X-Forwarded-For");

////  Just for testing

       // String ips = DSpaceServicesFactory.getInstance().getConfigurationService()
       //                                           .getProperty("ip.MusicFullIPs");
       // LOGGER.info ("JOSEAUTHips: the ips from config = " + ips);
       // final String[] MusicFullIPs = ips.split("\\|");

       // for (int i = 0; i < MusicFullIPs.length; i++)
       // {
       //      LOGGER.info ("JOSEAUTH: MusicFullIPs = " + MusicFullIPs[i]);
       // }

       // ips = DSpaceServicesFactory.getInstance().getConfigurationService()
       //                                           .getProperty("ip.MusicLowerHalfIPs");
       // LOGGER.info ("JOSEAUTHips: the ips from config = " + ips);
       // final String[] MusicLowerHalfIPs = ips.split("\\|");

       // for (int i = 0; i < MusicLowerHalfIPs.length; i++)
       // {
       //      LOGGER.info ("JOSEAUTH: MusicLowerHalfIPs = " + MusicLowerHalfIPs[i]);
       // }

       // ips = DSpaceServicesFactory.getInstance().getConfigurationService()
       //                                            .getProperty("ip.MusicUpperHalfIPs");

       // LOGGER.info ("JOSEAUTHips: the ips from config = " + ips);
       // final String[] MusicUpperHalfIPs = ips.split("\\|");

       //  for (int i = 0; i < MusicUpperHalfIPs.length; i++)
       //  {
       //      LOGGER.info ("JOSEAUTH: MusicUpperHalfIPs = " + MusicUpperHalfIPs[i]);
       //  }

////  Just for testing ends.

        if ( addr == null )
        {
            return false;
        }


       String ips = DSpaceServicesFactory.getInstance().getConfigurationService()
                                                 .getProperty("ip.MusicFullIPs");
       LOGGER.info ("JOSEAUTHips: the ips from config = " + ips);
       final String[] MusicFullIPs = ips.split("\\|");

       for (int i = 0; i < MusicFullIPs.length; i++)
       {
            LOGGER.info ("JOSEAUTH: MusicFullIPs = " + MusicFullIPs[i]);
       }

        for (int i = 0; i < MusicFullIPs.length; i++)
        {
            if (addr.startsWith(MusicFullIPs[i]))
            {
                return true;
            }
        }

       ips = DSpaceServicesFactory.getInstance().getConfigurationService()
                                                 .getProperty("ip.MusicLowerHalfIPs");
       LOGGER.info ("JOSEAUTHips: the ips from config = " + ips);
       final String[] MusicLowerHalfIPs = ips.split("\\|");

       for (int i = 0; i < MusicLowerHalfIPs.length; i++)
       {
            LOGGER.info ("JOSEAUTH: MusicLowerHalfIPs = " + MusicLowerHalfIPs[i]);
       }

        int count = 1;
        for (int i = 0; i < MusicLowerHalfIPs.length; i++)
        {
            while ( count < 127 )
            {
                if (addr.equals( MusicLowerHalfIPs[i] + Integer.toString(count) ) )
                {
                    return true;
                }
                count = count + 1;
            }
            count = 1;
        }


        ips = DSpaceServicesFactory.getInstance().getConfigurationService()
                                                  .getProperty("ip.MusicUpperHalfIPs");

       LOGGER.info ("JOSEAUTHips: the ips from config = " + ips);
       final String[] MusicUpperHalfIPs = ips.split("\\|");

        for (int i = 0; i < MusicUpperHalfIPs.length; i++)
        {
            LOGGER.info ("JOSEAUTH: MusicUpperHalfIPs = " + MusicUpperHalfIPs[i]);
        }

        count = 129;
        for (int i = 0; i < MusicUpperHalfIPs.length; i++)
        {
             LOGGER.info ("JOSEAUTH: the Upper Half IPS = " + MusicUpperHalfIPs[i]);
             while ( count < 255 )
             {
                 if (addr.equals( MusicUpperHalfIPs[i] + Integer.toString(count) ) )

                 {
                     return true;
                 }
                 count = count + 1;
             }
             count = 129;
        }

        return false;
    }


   public static boolean isBentleyOnlyUser(Context context, HttpServletRequest request)
    {
        //String addr = request.getRemoteAddr();
        String addr = request.getHeader("X-Forwarded-For");

       String ips = DSpaceServicesFactory.getInstance().getConfigurationService()
                                                  .getProperty("ip.BentleyOnlyIPs");

       LOGGER.info ("JOSEAUTHips: the ips from config = " + ips);
       final String[] BentleyOnlyIPs = ips.split("\\|");

        for (int i = 0; i < BentleyOnlyIPs.length; i++)
        {
            LOGGER.info ("JOSEAUTH: BentleyOnlyIPs = " + BentleyOnlyIPs[i]);
        }

        if ( addr == null )
        {
            return false;
        }

        int count = 0;
        for (int i = 0; i < BentleyOnlyIPs.length; i++)
        {
            while ( count < 128 )
            {
                if (addr.equals( BentleyOnlyIPs[i] + Integer.toString(count) ) )

                {
                    return true;
                }
                count = count + 1;
            }
        }


        return false;
    }

    public static boolean hasUMPriviledges(Context context)
    {

        String api_key = DSpaceServicesFactory.getInstance().getConfigurationService()
                                                 .getProperty("api.user.key");

        try
        {
        EPerson eperson = context.getCurrentUser();
        String email = "noemail@umich.edu";
        if ( eperson != null )
            {
                email = eperson.getEmail ();
            }
        else
            {
                return false;
            }

        // http://www.unix.org.ua/orelly/java-ent/jnut/ch04_02.htm  good page about
        // manipulating strings.
        // Now emove the @xxxx.xxx from the email
        int pos = email.indexOf('@');
        if ( pos > 0 )
            {
                String userid = email.substring(0,pos); // Extract the userid
                String request_url = "https://api-na.hosted.exlibrisgroup.com/almaws/v1/users/" + userid + "?apikey=" + api_key;

                URL url = new URL(request_url);

                // Get an input stream for reading
                InputStream in = url.openStream();

                // Create a buffered input stream for efficency
                BufferedInputStream bufIn = new BufferedInputStream(in);

                StringBuffer ReturnedValue = new StringBuffer("");
                for (;;)
                    {
                        int data = bufIn.read();

                        // Check for EOF
                        if (data == -1)
                            {break;}
                        else
                            {
                                ReturnedValue.append ( (char) data );
                            }
                    }
                String ResponseValue = ReturnedValue.toString();
                int pos2 = ResponseValue.indexOf("Error in Verification");
                if ( pos2 > 0 )
                    {
                        LOGGER.info ("OIDC: isues with verification...Error with verification");
                        return false;
                    }
                else
                    {
                        // Now check for:
                        //  <z303-budget>UMAA - Ann Arbor
                        //  <z303-budget>UMFL - Flint
                        //  <z303-budget>UMDB - Dearborn
                        int posUM = ResponseValue.indexOf("UMAA</campus_code>");
                        int posFL = ResponseValue.indexOf("UMFL</campus_code>");
                        int posDB = ResponseValue.indexOf("UMDB</campus_code>");
                        if ( ( posUM > 0 ) || ( posFL > 0 ) || ( posDB > 0 ) )
                        {
                            // Has UM permissions
                            LOGGER.info ("API: UM Person");
                            return true;
                        }
                        else
                        {
                            LOGGER.info ("API: Not a UM Person");
                            return false;
                        }
                    }
            }

        }
        catch (MalformedURLException mue)
        {
            System.err.println ("Invalid URL");
        }
        catch (IOException ioe)
        {
            System.err.println ("I/O Error - " + ioe);
        }

        LOGGER.info ("API: at the end");
        return false;
    }

    @Override
    public String getName() {
        return OIDC_AUTH_ATTRIBUTE;
    }

    @Override
    public int authenticate(Context context, String username, String password, String realm, HttpServletRequest request)
        throws SQLException {

//For Testing
//int a =0;
//int b=10;
//int c = b/a;

        if (request == null) {
            LOGGER.warn("Unable to authenticate using OIDC because the request object is null.");
            return BAD_ARGS;
        }

        if (request.getAttribute(OIDC_AUTH_ATTRIBUTE) == null) {
            return NO_SUCH_USER;
        }

        String code = (String) request.getParameter("code");
        if (StringUtils.isEmpty(code)) {
            LOGGER.warn("The incoming request has not code parameter");
            return NO_SUCH_USER;
        }

        return authenticateWithOidc(context, code, request);
    }

    private int authenticateWithOidc(Context context, String code, HttpServletRequest request) throws SQLException {

        OidcTokenResponseDTO accessToken = getOidcAccessToken(code);
        if (accessToken == null) {
            LOGGER.warn("No access token retrieved by code");
            return NO_SUCH_USER;
        }

        Map<String, Object> userInfo = getOidcUserInfo(accessToken.getAccessToken());

        String email = getAttributeAsString(userInfo, getEmailAttribute());
        if (StringUtils.isBlank(email)) {
            LOGGER.warn("No email found in the user info attributes");
            return NO_SUCH_USER;
        }

        EPerson ePerson = ePersonService.findByEmail(context, email);
        if (ePerson != null) {
            request.setAttribute(OIDC_AUTHENTICATED, true);
            return ePerson.canLogIn() ? logInEPerson(context, ePerson) : BAD_ARGS;
        }

        // if self registration is disabled, warn about this failure to find a matching eperson
        if (! canSelfRegister()) {
            LOGGER.warn("Self registration is currently disabled for OIDC, and no ePerson could be found for email: {}",
                email);
        }

        return canSelfRegister() ? registerNewEPerson(context, userInfo, email) : NO_SUCH_USER;
    }

    @Override
    public String loginPageURL(Context context, HttpServletRequest request, HttpServletResponse response) {

        String authorizeUrl = configurationService.getProperty("authentication-oidc.authorize-endpoint");
        String clientId = configurationService.getProperty("authentication-oidc.client-id");
        String clientSecret = configurationService.getProperty("authentication-oidc.client-secret");
        String redirectUri = configurationService.getProperty("authentication-oidc.redirect-url");
        String tokenUrl = configurationService.getProperty("authentication-oidc.token-endpoint");
        String userInfoUrl = configurationService.getProperty("authentication-oidc.user-info-endpoint");
        String[] defaultScopes =
            new String[] {
                "openid", "email", "profile"
            };
        String scopes = String.join(" ", configurationService.getArrayProperty("authentication-oidc.scopes",
            defaultScopes));

        if (isAnyBlank(authorizeUrl, clientId, redirectUri, clientSecret, tokenUrl, userInfoUrl)) {
            LOGGER.error("Missing mandatory configuration properties for OidcAuthenticationBean");

            // prepare a Map of the properties which can not have sane defaults, but are still required
            final Map<String, String> map = Map.of("authorizeUrl", authorizeUrl, "clientId", clientId, "redirectUri",
                redirectUri, "clientSecret", clientSecret, "tokenUrl", tokenUrl, "userInfoUrl", userInfoUrl);
            final Iterator<Entry<String, String>> iterator = map.entrySet().iterator();

            while (iterator.hasNext()) {
                final Entry<String, String> entry = iterator.next();

                if (isBlank(entry.getValue())) {
                    LOGGER.error(" * {} is missing", entry.getKey());
                }
            }
            return "";
        }

        try {
            LOGGER.warn("AUTHJOSE: LOGIN ==> " + format(LOGIN_PAGE_URL_FORMAT, authorizeUrl, clientId, scopes, encode(redirectUri, "UTF-8")));
            return format(LOGIN_PAGE_URL_FORMAT, authorizeUrl, clientId, scopes, encode(redirectUri, "UTF-8"));
        } catch (UnsupportedEncodingException e) {
            LOGGER.error(e.getMessage(), e);
            return "";
        }

    }

    private int logInEPerson(Context context, EPerson ePerson) {
        context.setCurrentUser(ePerson);
        return SUCCESS;
    }

    private int registerNewEPerson(Context context, Map<String, Object> userInfo, String email) throws SQLException {
        try {

            context.turnOffAuthorisationSystem();

            EPerson eperson = ePersonService.create(context);

            eperson.setNetid(email);
            eperson.setEmail(email);

            String firstName = getAttributeAsString(userInfo, getFirstNameAttribute());
            if (firstName != null) {
                eperson.setFirstName(context, firstName);
            }

            String lastName = getAttributeAsString(userInfo, getLastNameAttribute());
            if (lastName != null) {
                eperson.setLastName(context, lastName);
            }

            eperson.setCanLogIn(true);
            eperson.setSelfRegistered(true);

            ePersonService.update(context, eperson);
            context.setCurrentUser(eperson);
            context.dispatchEvents();

            return SUCCESS;

        } catch (Exception ex) {
            LOGGER.error("An error occurs registering a new EPerson from OIDC", ex);
            return NO_SUCH_USER;
        } finally {
            context.restoreAuthSystemState();
        }
    }

    private OidcTokenResponseDTO getOidcAccessToken(String code) {
        try {


            LOGGER.error("AUTHJOSE:  Trying to get oidc access token with this code = " + code);
            return oidcClient.getAccessToken(code);
        } catch (Exception ex) {
            LOGGER.error("An error occurs retriving the OIDC access_token", ex);
            return null;
        }
    }

    private Map<String, Object> getOidcUserInfo(String accessToken) {
        try {
            return oidcClient.getUserInfo(accessToken);
        } catch (Exception ex) {
            LOGGER.error("An error occurs retriving the OIDC user info", ex);
            return Map.of();
        }
    }

    private String getAttributeAsString(Map<String, Object> userInfo, String attribute) {
        if (isBlank(attribute)) {
            return null;
        }
        return userInfo.containsKey(attribute) ? String.valueOf(userInfo.get(attribute)) : null;
    }

    private String getEmailAttribute() {
        return configurationService.getProperty("authentication-oidc.user-info.email", "email");
    }

    private String getFirstNameAttribute() {
        return configurationService.getProperty("authentication-oidc.user-info.first-name", "given_name");
    }

    private String getLastNameAttribute() {
        return configurationService.getProperty("authentication-oidc.user-info.last-name", "family_name");
    }

    private boolean canSelfRegister() {
        String canSelfRegister = configurationService.getProperty("authentication-oidc.can-self-register", "true");
        if (isBlank(canSelfRegister)) {
            return true;
        }
        return toBoolean(canSelfRegister);
    }

    public OidcClient getOidcClient() {
        return this.oidcClient;
    }

    public void setOidcClient(OidcClient oidcClient) {
        this.oidcClient = oidcClient;
    }

    @Override
    public boolean isUsed(final Context context, final HttpServletRequest request) {
        if (request != null &&
                context.getCurrentUser() != null &&
                request.getAttribute(OIDC_AUTHENTICATED) != null) {
            return true;
        }
        return false;
    }

    @Override
    public boolean canChangePassword(Context context, EPerson ePerson, String currentPassword) {
        return false;
    }

}
