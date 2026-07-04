<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet version="1.0"
  xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
  xmlns:fo="http://www.w3.org/1999/XSL/Format"
  xmlns:db="http://docbook.org/ns/docbook">

  <xsl:output method="xml" encoding="UTF-8" indent="yes"/>

  <xsl:template match="/">
    <fo:root>
      <fo:layout-master-set>
        <fo:simple-page-master master-name="license" page-width="5.5in" page-height="8in"
          margin-top="0.12in" margin-bottom="0.12in" margin-left="0.12in" margin-right="0.12in">
          <fo:region-body/>
        </fo:simple-page-master>
      </fo:layout-master-set>
      <fo:page-sequence master-reference="license">
        <fo:flow flow-name="xsl-region-body" font-family="Arial" font-size="8pt" line-height="10pt">
          <xsl:apply-templates/>
        </fo:flow>
      </fo:page-sequence>
    </fo:root>
  </xsl:template>

  <xsl:template match="db:info|db:toc|db:indexterm"/>

  <xsl:template match="db:title">
    <fo:block font-size="12pt" font-weight="bold" space-before="5pt" space-after="4pt" keep-with-next.within-page="always">
      <xsl:apply-templates/>
    </fo:block>
  </xsl:template>

  <xsl:template match="db:section/db:title">
    <fo:block font-size="10pt" font-weight="bold" space-before="8pt" space-after="4pt" keep-with-next.within-page="always">
      <xsl:apply-templates/>
    </fo:block>
  </xsl:template>

  <xsl:template match="db:para|db:simpara">
    <fo:block space-after="4pt">
      <xsl:apply-templates/>
    </fo:block>
  </xsl:template>

  <xsl:template match="db:note">
    <fo:block space-before="4pt" space-after="4pt">
      <fo:inline font-weight="bold">Note: </fo:inline>
      <xsl:apply-templates/>
    </fo:block>
  </xsl:template>

  <xsl:template match="db:itemizedlist">
    <fo:list-block provisional-distance-between-starts="0.18in" provisional-label-separation="0.05in" space-after="4pt">
      <xsl:apply-templates/>
    </fo:list-block>
  </xsl:template>

  <xsl:template match="db:listitem">
    <fo:list-item>
      <fo:list-item-label end-indent="label-end()">
        <fo:block>-</fo:block>
      </fo:list-item-label>
      <fo:list-item-body start-indent="body-start()">
        <xsl:apply-templates/>
      </fo:list-item-body>
    </fo:list-item>
  </xsl:template>

  <xsl:template match="db:table|db:informaltable">
    <fo:block space-before="4pt" space-after="6pt">
      <xsl:if test="db:title">
        <fo:block font-weight="bold" space-after="3pt">
          <xsl:apply-templates select="db:title/node()"/>
        </fo:block>
      </xsl:if>
      <fo:table table-layout="fixed" width="5.26in" font-size="7pt" line-height="8.5pt">
        <fo:table-column column-width="1.84in"/>
        <fo:table-column column-width="0.95in"/>
        <fo:table-column column-width="2.47in"/>
        <xsl:apply-templates select="db:tgroup"/>
      </fo:table>
    </fo:block>
  </xsl:template>

  <xsl:template match="db:tgroup">
    <xsl:apply-templates select="db:thead"/>
    <xsl:apply-templates select="db:tbody"/>
  </xsl:template>

  <xsl:template match="db:thead">
    <fo:table-header>
      <xsl:apply-templates/>
    </fo:table-header>
  </xsl:template>

  <xsl:template match="db:tbody">
    <fo:table-body>
      <xsl:apply-templates/>
    </fo:table-body>
  </xsl:template>

  <xsl:template match="db:row">
    <fo:table-row>
      <xsl:apply-templates/>
    </fo:table-row>
  </xsl:template>

  <xsl:template match="db:entry">
    <fo:table-cell border="0.2pt solid #999999" padding="2pt" overflow="hidden">
      <fo:block wrap-option="wrap">
        <xsl:apply-templates/>
      </fo:block>
    </fo:table-cell>
  </xsl:template>

  <xsl:template match="db:thead//db:entry">
    <fo:table-cell border="0.2pt solid #777777" padding="2pt" background-color="#eeeeee">
      <fo:block font-weight="bold" wrap-option="wrap">
        <xsl:apply-templates/>
      </fo:block>
    </fo:table-cell>
  </xsl:template>

  <xsl:template match="db:programlisting|db:screen">
    <fo:block font-family="Courier New" font-size="7pt" line-height="8.5pt"
      white-space-collapse="false" wrap-option="wrap" space-before="3pt" space-after="5pt">
      <xsl:value-of select="."/>
    </fo:block>
  </xsl:template>

  <xsl:template match="db:link|db:ulink|db:literal|db:code|db:phrase|db:emphasis">
    <xsl:apply-templates/>
  </xsl:template>

  <xsl:template match="text()">
    <xsl:value-of select="."/>
  </xsl:template>
</xsl:stylesheet>
