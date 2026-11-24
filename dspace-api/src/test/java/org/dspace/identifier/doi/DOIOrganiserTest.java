/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.identifier.doi;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.sql.SQLException;

import org.dspace.AbstractDSpaceTest;
import org.dspace.content.DSpaceObject;
import org.dspace.core.Constants;
import org.dspace.core.Context;
import org.dspace.identifier.DOI;
import org.dspace.identifier.DOIIdentifierProvider;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class DOIOrganiserTest extends AbstractDSpaceTest {
    private static final String IDENTIFIER = "doi:10.7302/example";

    private final ByteArrayOutputStream output = new ByteArrayOutputStream();
    private PrintStream originalOut;
    private PrintStream originalErr;
    private PrintStream capturedOutput;
    private DOIIdentifierProvider provider;
    private DOIOrganiser organiser;
    private DOI doi;
    private DSpaceObject dso;
    private Context context;

    @Before
    public void setUp() {
        context = mock(Context.class);
        provider = mock(DOIIdentifierProvider.class);
        organiser = new DOIOrganiser(context, provider);
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

        organiser.update(doi);

        assertTrue(output.toString().contains("Updating metadata of DOI " + IDENTIFIER));
        assertTrue(output.toString().contains("It wasn't possible to update this identifier: " + IDENTIFIER));
    }
}
