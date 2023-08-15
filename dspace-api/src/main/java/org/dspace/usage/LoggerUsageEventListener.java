/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.usage;

import org.apache.logging.log4j.Logger;
import org.dspace.content.DSpaceObject;
import org.dspace.content.Item;
import org.dspace.core.Constants;
import org.dspace.core.LogHelper;
import org.dspace.services.model.Event;
import org.dspace.usage.UsageEvent.Action;


import org.dspace.content.Bitstream;
import org.dspace.content.Bundle;

/**
 * @author Mark Diggory (mdiggory at atmire.com)
 */
public class LoggerUsageEventListener extends AbstractUsageEventListener {

    /**
     * log4j category
     */
    private static Logger log = org.apache.logging.log4j.LogManager.getLogger(LoggerUsageEventListener.class);

    @Override
    public void receiveEvent(Event event) {

        //Search events are already logged
        //UsageSearchEvent is already logged in the search classes, no need to repeat this logging
        if (event instanceof UsageEvent && !(event instanceof UsageSearchEvent)) {
            UsageEvent ue = (UsageEvent) event;

            if (ue.getObject() instanceof Bitstream) {
                log.info(LogHelper.getHeader2(
                    ue.getContextSpecial(),
                    "view_bitstream_details",
                    formatMessage(ue.getObject()),
                    formatMessage2(ue.getObject()))
                );
            } else if((ue.getObject() instanceof Item) ) {
                 log.info(LogHelper.getHeader2(
                    ue.getContextSpecialItem(),
                    "view_item_details",
                    formatMessage(ue.getObject()),
                    formatMessage2(ue.getObject()))
                );
            }
            else {
                log.info(LogHelper.getHeader(
                    ue.getContextSpecial(),
                    formatAction(ue.getAction(), ue.getObject()),
                    formatMessage(ue.getObject()))
                );     

            }

        }
    }

    private static String formatAction(Action action, DSpaceObject object) {
        try {
            String objText = Constants.typeText[object.getType()].toLowerCase();
            return action.text() + "_" + objText;
        } catch (Exception e) {
            // ignore
        }
        return "";

    }

    private static String formatMessage(DSpaceObject object) {
        try {
            String objText = Constants.typeText[object.getType()].toLowerCase();
            String handle = object.getHandle();

            /* Emulate Item logger */
            if (handle != null && object instanceof Item) {
                return "handle=" + object.getHandle();
            } else if (object instanceof Bitstream) {
                Bitstream bitstream = (Bitstream) object;
                Bundle bundle = bitstream.getBundles().get(0);
                Item item = bundle.getItems().get(0);
                handle = item.getHandle();
                return handle;

            } else {
                return objText + "_id=" + object.getID();
            }

        } catch (Exception e) {
            // ignore
        }
        return "";

    }

    private static String formatMessage2(DSpaceObject object) {
        try {
            String objText = Constants.typeText[object.getType()].toLowerCase();
            String handle = object.getHandle();

            /* Emulate Item logger */
            if (handle != null && object instanceof Item) {
                return "handle=" + object.getHandle();
            } else if (object instanceof Bitstream) {
                Bitstream bitstream = (Bitstream) object;
                String filename = bitstream.getName();
                return filename;

            } else {
                return objText + "_id=" + object.getID();
            }

        } catch (Exception e) {
            // ignore
        }
        return "";

    }


}
