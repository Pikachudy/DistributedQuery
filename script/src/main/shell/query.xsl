<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform">
    <xsl:output method="xml" indent="yes"/>

    <xsl:template match="/">
        <root>
            <xsl:for-each select="//article[contains(author, '$author') and year>=$year_min and year&lt;=$year_max]">
                <xsl:copy-of select="."/>
            </xsl:for-each>
        </root>
    </xsl:template>

<!--    <!DOCTYPE root SYSTEM "dblp.dtd">-->
</xsl:stylesheet>