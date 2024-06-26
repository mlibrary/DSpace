/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.solrdatabaseresync;

import static org.dspace.discovery.indexobject.ItemIndexFactoryImpl.STATUS_FIELD;
import static org.dspace.discovery.indexobject.ItemIndexFactoryImpl.STATUS_FIELD_PREDB;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

import java.util.List;

import org.apache.commons.collections.CollectionUtils;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrDocumentList;
import org.dspace.AbstractIntegrationTestWithDatabase;
import org.dspace.app.launcher.ScriptLauncher;
import org.dspace.app.scripts.handler.impl.TestDSpaceRunnableHandler;
import org.dspace.builder.CollectionBuilder;
import org.dspace.builder.CommunityBuilder;
import org.dspace.builder.ItemBuilder;
import org.dspace.content.Collection;
import org.dspace.content.Item;
import org.dspace.content.factory.ContentServiceFactory;
import org.dspace.content.service.CollectionService;
import org.dspace.discovery.MockSolrSearchCore;
import org.dspace.kernel.ServiceManager;
import org.dspace.services.ConfigurationService;
import org.dspace.services.factory.DSpaceServicesFactory;
import org.junit.Before;
import org.junit.Test;

/**
 * IT for {@link org.dspace.app.solrdatabaseresync.SolrDatabaseResyncIT}
 */
public class SolrDatabaseResyncIT extends AbstractIntegrationTestWithDatabase {

    private final ConfigurationService configurationService =
            DSpaceServicesFactory.getInstance().getConfigurationService();

    private final CollectionService collectionService =
            ContentServiceFactory.getInstance().getCollectionService();

    private MockSolrSearchCore searchService;

    private Collection col;
    private Item item1;
    private Item item2;
    private Item item3;
    private Item item4;
    private Item item5;
    private Item item6;
    private Item item7;
    private Item item8;
    private Item item9;
    private Item item10;
    private Item item11;

    @Before
    public void setUp() throws Exception {
        super.setUp();
        configurationService.setProperty("solr-database-resync.time-until-reindex", 1);
        configurationService.setProperty("script.solr-database-resync.batch-size", 5);

        ServiceManager serviceManager = DSpaceServicesFactory.getInstance().getServiceManager();
        searchService = serviceManager.getServiceByName(null, MockSolrSearchCore.class);

        context.turnOffAuthorisationSystem();

        parentCommunity = CommunityBuilder.createCommunity(context).withName("Parent Community").build();
        col = CollectionBuilder.createCollection(context, parentCommunity).withName("Collection").build();

        item1 = ItemBuilder.createItem(context, col)
                .withTitle("Public item 1")
                .withIssueDate("2010-10-17")
                .withAuthor("Smith, Donald")
                .withSubject("ExtraEntry")
                .build();
        item2 = ItemBuilder.createItem(context, col)
                .withTitle("Public item 2")
                .withIssueDate("2011-08-13")
                .withAuthor("Smith, Maria")
                .withSubject("TestingForMore")
                .build();

        item3 = ItemBuilder.createItem(context, col).withTitle("Public item 3").build();
        item4 = ItemBuilder.createItem(context, col).withTitle("Public item 4").build();
        item5 = ItemBuilder.createItem(context, col).withTitle("Public item 5").build();
        item6 = ItemBuilder.createItem(context, col).withTitle("Public item 6").build();
        item7 = ItemBuilder.createItem(context, col).withTitle("Public item 7").build();
        item8 = ItemBuilder.createItem(context, col).withTitle("Public item 8").build();
        item9 = ItemBuilder.createItem(context, col).withTitle("Public item 9").build();
        item10 = ItemBuilder.createItem(context, col).withTitle("Public item 10").build();
        item11 = ItemBuilder.createItem(context, col).withTitle("Public item 11").build();

        context.setDispatcher("noindex");
    }

    @Test
    public void solrPreDBStatusExistingItemTest() throws Exception {
        // Items were created, they should contain a predb status in solr
        assertHasPreDBStatus(item1);
        assertHasPreDBStatus(item2);
        assertHasPreDBStatus(item3);
        assertHasPreDBStatus(item4);
        assertHasPreDBStatus(item5);
        assertHasPreDBStatus(item6);
        assertHasPreDBStatus(item7);
        assertHasPreDBStatus(item8);
        assertHasPreDBStatus(item9);
        assertHasPreDBStatus(item10);
        assertHasPreDBStatus(item11);

        performSolrDatabaseResyncScript();

        // Database status script was performed, their predb status should be removed
        assertHasNoPreDBStatus(item1);
        assertHasNoPreDBStatus(item2);
        assertHasNoPreDBStatus(item3);
        assertHasNoPreDBStatus(item4);
        assertHasNoPreDBStatus(item5);
        assertHasNoPreDBStatus(item6);
        assertHasNoPreDBStatus(item7);
        assertHasNoPreDBStatus(item8);
        assertHasNoPreDBStatus(item9);
        assertHasNoPreDBStatus(item10);
        assertHasNoPreDBStatus(item11);

        context.restoreAuthSystemState();
    }

    @Test
    public void solrPreDBStatusRemovedItemTest() throws Exception {
        // Items were created, they should contain a predb status in solr
        assertHasPreDBStatus(item1);
        assertHasPreDBStatus(item2);
        assertHasPreDBStatus(item3);
        assertHasPreDBStatus(item4);
        assertHasPreDBStatus(item5);
        assertHasPreDBStatus(item6);
        assertHasPreDBStatus(item7);
        assertHasPreDBStatus(item8);
        assertHasPreDBStatus(item9);
        assertHasPreDBStatus(item10);
        assertHasPreDBStatus(item11);

        collectionService.delete(context, col);

        // Items were deleted, they should still contain a predb status in solr for now
        assertHasPreDBStatus(item1);
        assertHasPreDBStatus(item2);
        assertHasPreDBStatus(item3);
        assertHasPreDBStatus(item4);
        assertHasPreDBStatus(item5);
        assertHasPreDBStatus(item6);
        assertHasPreDBStatus(item7);
        assertHasPreDBStatus(item8);
        assertHasPreDBStatus(item9);
        assertHasPreDBStatus(item10);
        assertHasPreDBStatus(item11);

        performSolrDatabaseResyncScript();

        // Database status script was performed, their solr document should have been removed
        assertNoSolrDocument(item1);
        assertNoSolrDocument(item2);
        assertNoSolrDocument(item3);
        assertNoSolrDocument(item4);
        assertNoSolrDocument(item5);
        assertNoSolrDocument(item6);
        assertNoSolrDocument(item7);
        assertNoSolrDocument(item8);
        assertNoSolrDocument(item9);
        assertNoSolrDocument(item10);
        assertNoSolrDocument(item11);

        context.restoreAuthSystemState();
    }


    public void assertHasNoPreDBStatus(Item item) throws Exception {
        assertNotEquals(STATUS_FIELD_PREDB, getStatus(item));
    }

    public void assertHasPreDBStatus(Item item) throws Exception {
        assertEquals(STATUS_FIELD_PREDB, getStatus(item));
    }

    public void assertNoSolrDocument(Item item) throws Exception {
        SolrDocumentList solrDocumentList = getSolrDocumentList(item);
        assertEquals(0, solrDocumentList.size());
    }

    public String getStatus(Item item) throws Exception {
        SolrDocumentList solrDocumentList = getSolrDocumentList(item);
        List fieldValues = ((List) solrDocumentList.get(0).getFieldValues(STATUS_FIELD));
        if (CollectionUtils.isNotEmpty(fieldValues)) {
            return (String) fieldValues.get(0);
        } else {
            return null;
        }
    }

    public SolrDocumentList getSolrDocumentList(Item item) throws Exception {
        SolrQuery solrQuery = new SolrQuery();
        solrQuery.setQuery("search.resourceid:" + item.getID());
        QueryResponse queryResponse = searchService.getSolr().query(solrQuery);
        return queryResponse.getResults();
    }

    public void performSolrDatabaseResyncScript() throws Exception {
        String[] args = new String[] {"solr-database-resync"};
        TestDSpaceRunnableHandler testDSpaceRunnableHandler = new TestDSpaceRunnableHandler();
        ScriptLauncher
                .handleScript(args, ScriptLauncher.getConfig(kernelImpl), testDSpaceRunnableHandler, kernelImpl);
    }

}
