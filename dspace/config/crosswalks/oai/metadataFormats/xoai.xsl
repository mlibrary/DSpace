<?xml version="1.0" encoding="UTF-8" ?>
<xsl:stylesheet 
    xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
    xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
    xmlns:doc="http://www.lyncode.com/xoai"
    exclude-result-prefixes="xsi doc"
    version="1.0">
    
    <xsl:output omit-xml-declaration="yes" method="xml" indent="yes" />

    <!-- Main template -->
    <xsl:template match="/doc:metadata">
        <your:outputRoot xmlns:your="http://www.example.com/your/namespace">
            <xsl:apply-templates
              select="descendant::doc:field[@name='value' and not(
                ancestor::doc:element[@name='other' and parent::doc:element[@name='identifier']]
              )]"
            />
        </your:outputRoot>
    </xsl:template>
    
    <!-- Output only the text, drop the attribute -->
    <xsl:template match="doc:field">
        <field xmlns="http://www.lyncode.com/xoai">
            <xsl:value-of select="."/>
        </field>
    </xsl:template>

</xsl:stylesheet>