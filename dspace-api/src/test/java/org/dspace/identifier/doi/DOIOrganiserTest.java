/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.identifier.doi;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.CALLS_REAL_METHODS;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.Collections;

import org.dspace.content.DSpaceObject;
import org.dspace.core.Constants;
import org.dspace.core.Context;
import org.dspace.identifier.DOI;
import org.dspace.identifier.DOIIdentifierProvider;
import org.dspace.identifier.service.DOIService;
import org.dspace.services.ConfigurationService;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.test.util.ReflectionTestUtils;

public class DOIOrganiserTest {
    private static final String IDENTIFIER = "doi:10.7302/example";

    private final ByteArrayOutputStream output = new ByteArrayOutputStream();
    private PrintStream originalOut;
    private PrintStream originalErr;
    private PrintStream capturedOutput;
    private DOIIdentifierProvider provider;
    private DOIOrganiser organiser;
    private DOIService doiService;
    private DOI doi;
    private DSpaceObject dso;
    private Context context;

    @Before
    public void setUp() {
        context = mock(Context.class);
        provider = mock(DOIIdentifierProvider.class);
        organiser = mock(DOIOrganiser.class, CALLS_REAL_METHODS);
        ReflectionTestUtils.setField(organiser, "context", context);
        ReflectionTestUtils.setField(organiser, "provider", provider);
        doiService = mock(DOIService.class);
        organiser.doiService = doiService;
        organiser.configurationService = mock(ConfigurationService.class);
        doi = mock(DOI.class);
        dso = mock(DSpaceObject.class);
        when(doi.getDoi()).thenReturn("10.7302/example");
        originalOut = System.out;
        originalErr = System.err;
        capturedOutput = new PrintStream(output, true);
        System.setOut(capturedOutput);
        System.setErr(capturedOutput);
    }

    @After
    public void tearDown() {
        System.setOut(originalOut);
        System.setErr(originalErr);
        capturedOutput.close();
    }

    @Test
    public void reportsDoiBeforeSuccessfulUpdate() throws Exception {
        when(doi.getDSpaceObject()).thenReturn(dso);
        when(dso.getType()).thenReturn(Constants.ITEM);
        doAnswer(invocation -> {
            assertTrue(output.toString().contains("Updating metadata of DOI " + IDENTIFIER));
            return null;
        }).when(provider).updateMetadataOnline(context, dso, IDENTIFIER);

        organiser.update(doi);

        assertTrue(output.toString().contains("Successfully updated metadata of DOI " + IDENTIFIER));
    }

    @Test
    public void reportsDoiWhenObjectIsMissing() throws Exception {
        try {
            organiser.update(doi);
            fail("Expected an exception for a missing object");
        } catch (IllegalStateException ex) {
            assertTrue(ex.getMessage().contains(IDENTIFIER));
            assertTrue(ex.getMessage().contains("no associated DSpace object"));
        }

        assertTrue(output.toString().contains("Updating metadata of DOI " + IDENTIFIER));
        assertTrue(output.toString().contains("It wasn't possible to update this identifier: " + IDENTIFIER));
        verify(provider, never()).updateMetadataOnline(any(), any(), eq(IDENTIFIER));
    }

    @Test
    public void reportsDoiWhenDatabaseUpdateFails() throws Exception {
        when(doi.getDSpaceObject()).thenReturn(dso);
        when(dso.getType()).thenReturn(Constants.ITEM);
        doThrow(new SQLException("database unavailable"))
            .when(provider).updateMetadataOnline(context, dso, IDENTIFIER);

        try {
            organiser.update(doi);
            fail("Expected a database update failure");
        } catch (IllegalStateException ex) {
            assertTrue(ex.getCause() instanceof SQLException);
        }

        assertTrue(output.toString().contains("Updating metadata of DOI " + IDENTIFIER));
        assertTrue(output.toString().contains("It wasn't possible to update this identifier: " + IDENTIFIER));
    }

    @Test
    public void continuesQueuedUpdatesAfterMissingObjectAndReturnsFailure() throws Exception {
        DOI validDoi = mock(DOI.class);
        when(validDoi.getDoi()).thenReturn("10.7302/valid");
        when(validDoi.getDSpaceObject()).thenReturn(dso);
        when(dso.getType()).thenReturn(Constants.ITEM);
        when(doiService.getDOIsByStatus(eq(context), any())).thenReturn(Arrays.asList(doi, validDoi));

        assertEquals(1, DOIOrganiser.runCLI(context, organiser, new String[] {"-u"}));

        verify(provider).updateMetadataOnline(context, dso, "doi:10.7302/valid");
        verify(context).uncacheEntity(doi);
        verify(context).uncacheEntity(validDoi);
        assertTrue(output.toString().contains("Failed to update DOI " + IDENTIFIER));
        assertTrue(output.toString().contains("Successfully updated metadata of DOI doi:10.7302/valid"));
    }

    @Test
    public void returnsSuccessForSuccessfulUpdateQueue() throws Exception {
        when(doi.getDSpaceObject()).thenReturn(dso);
        when(dso.getType()).thenReturn(Constants.ITEM);
        when(doiService.getDOIsByStatus(eq(context), any())).thenReturn(Collections.singletonList(doi));

        assertEquals(0, DOIOrganiser.runCLI(context, organiser, new String[] {"-u"}));

        verify(provider).updateMetadataOnline(context, dso, IDENTIFIER);
        verify(context).uncacheEntity(doi);
    }

    @Test
    public void continuesQueuedUpdatesAfterDatabaseUpdateFailure() throws Exception {
        DOI validDoi = mock(DOI.class);
        when(validDoi.getDoi()).thenReturn("10.7302/valid");
        when(validDoi.getDSpaceObject()).thenReturn(dso);
        when(doi.getDSpaceObject()).thenReturn(dso);
        when(dso.getType()).thenReturn(Constants.ITEM);
        when(doiService.getDOIsByStatus(eq(context), any())).thenReturn(Arrays.asList(doi, validDoi));
        doThrow(new SQLException("update failed")).when(provider).updateMetadataOnline(context, dso, IDENTIFIER);

        assertEquals(1, DOIOrganiser.runCLI(context, organiser, new String[] {"-u"}));

        verify(provider).updateMetadataOnline(context, dso, "doi:10.7302/valid");
        verify(context).uncacheEntity(doi);
        verify(context).uncacheEntity(validDoi);
    }

    @Test
    public void returnsSuccessForEmptyUpdateQueue() throws Exception {
        when(doiService.getDOIsByStatus(eq(context), any())).thenReturn(Collections.emptyList());

        assertEquals(0, DOIOrganiser.runCLI(context, organiser, new String[] {"-u"}));
    }

    @Test
    public void reportsQueuedUpdateFailureInQuietMode() throws Exception {
        when(doiService.getDOIsByStatus(eq(context), any())).thenReturn(Collections.singletonList(doi));

        assertEquals(1, DOIOrganiser.runCLI(context, organiser, new String[] {"-u", "-q"}));

        assertTrue(output.toString().contains("Failed to update DOI " + IDENTIFIER));
    }

    @Test
    public void returnsFailureWhenUpdateQueueCannotBeRead() throws Exception {
        when(doiService.getDOIsByStatus(eq(context), any())).thenThrow(new SQLException("database unavailable"));

        assertEquals(1, DOIOrganiser.runCLI(context, organiser, new String[] {"-u"}));

        assertTrue(output.toString().contains("database unavailable"));
    }

    @Test
    public void propagatesIdentifierUpdateFailure() throws Exception {
        when(doi.getDSpaceObject()).thenReturn(dso);
        when(dso.getType()).thenReturn(Constants.ITEM);
        DOIIdentifierException failure = new DOIIdentifierException("Update failed",
            DOIIdentifierException.BAD_REQUEST);
        doThrow(failure).when(provider).updateMetadataOnline(context, dso, IDENTIFIER);
        when(organiser.configurationService.getProperty("alert.recipient")).thenReturn(null);

        try {
            organiser.update(doi);
            fail("Expected an identifier update failure");
        } catch (IllegalStateException ex) {
            assertSame(failure, ex.getCause());
        }
    }
}
