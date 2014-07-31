<?xml version="1.0" encoding="UTF-8"?>  
<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform">  

		<xsl:template match="/">
			<add>
			<xsl:for-each select="//RECORD"> <!--per ogni record che trova aggiunge un doc e ci va ad inserire i campi-->
				<doc>
					<field name="id"><xsl:value-of select="DESCRIPTEUR_ID"/></field>
					<field name="name"><xsl:value-of select="LIBELLE"/></field>
				</doc>
			</xsl:for-each>  
			</add>
	
</xsl:template>  
</xsl:stylesheet>  