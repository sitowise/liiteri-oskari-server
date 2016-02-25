﻿<?xml version="1.0" encoding="ISO-8859-1" ?>
<StyledLayerDescriptor version="1.0.0" xmlns="http://www.opengis.net/sld" xmlns:ogc="http://www.opengis.net/ogc" xmlns:xlink="http://www.w3.org/1999/xlink" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xsi:schemaLocation="http://www.opengis.net/sld http://schemas.opengis.net/sld/1.0.0/StyledLayerDescriptor.xsd">
    <NamedLayer>
        <Name>sld_muutos_n1</Name>
        <UserStyle>
            <Title>Analysis delta</Title>
            <FeatureTypeStyle>
             <Rule>
                    <Title>Polygons  n1 equalto 0</Title>
                    <ogc:Filter>

                            <ogc:PropertyIsEqualTo>
                                <ogc:PropertyName>n1</ogc:PropertyName>
                                <ogc:Literal>0.0</ogc:Literal>
                            </ogc:PropertyIsEqualTo>
  
                    </ogc:Filter>
                    <PolygonSymbolizer>
                        <Fill>
                            <CssParameter name="fill">#FFFFFF</CssParameter>
                        </Fill>
                        <Stroke>
                            <CssParameter name="stroke">#C0C0C0</CssParameter>
                            <CssParameter name="stroke-width">1</CssParameter>
                        </Stroke>
                    </PolygonSymbolizer>
                </Rule>
              
                <Rule>
                    <Title>Polygons gt 0</Title>
                    <ogc:Filter>
            
                            <ogc:PropertyIsGreaterThan>
                                <ogc:PropertyName>n1</ogc:PropertyName>
                                <ogc:Literal>0.0</ogc:Literal>
                            </ogc:PropertyIsGreaterThan>
          
                    </ogc:Filter>
                    <PolygonSymbolizer>
                        <Fill>
                            <CssParameter name="fill">#CA0020</CssParameter>
                        </Fill>
                        <Stroke>
                            <CssParameter name="stroke">#C0C0C0</CssParameter>
                            <CssParameter name="stroke-width">1</CssParameter>
                        </Stroke>
                    </PolygonSymbolizer>
                </Rule>
              <Rule>
                    <Title>Polygons n1 lt 0 </Title>
                    <ogc:Filter>
                            <ogc:And>
                              <ogc:Not>
                                 <ogc:PropertyIsEqualTo>
                                <ogc:PropertyName>n1</ogc:PropertyName>
                                <ogc:Literal>-111111111.0</ogc:Literal>
                            </ogc:PropertyIsEqualTo>
                              </ogc:Not>
                            <ogc:PropertyIsLessThan>
                                <ogc:PropertyName>n1</ogc:PropertyName>
                                <ogc:Literal>0.0</ogc:Literal>
                            </ogc:PropertyIsLessThan>
                       </ogc:And>
                    </ogc:Filter>
                    <PolygonSymbolizer>
                        <Fill>
                            <CssParameter name="fill">#0571B0</CssParameter>
                        </Fill>
                        <Stroke>
                            <CssParameter name="stroke">#C0C0C0</CssParameter>
                            <CssParameter name="stroke-width">1</CssParameter>
                        </Stroke>
                    </PolygonSymbolizer>
                </Rule>
                 <Rule>
                    <Title>Polygons eq -111111111</Title>
                    <ogc:Filter>
            
                             <ogc:PropertyIsEqualTo>
                                <ogc:PropertyName>n1</ogc:PropertyName>
                                <ogc:Literal>-111111111.0</ogc:Literal>
                            </ogc:PropertyIsEqualTo>
          
                    </ogc:Filter>
                    <PolygonSymbolizer>
                        <Fill>
                            <CssParameter name="fill">#707070</CssParameter>
                        </Fill>
                        <Stroke>
                            <CssParameter name="stroke">#C0C0C0</CssParameter>
                            <CssParameter name="stroke-width">1</CssParameter>
                        </Stroke>
                    </PolygonSymbolizer>
                </Rule>
            </FeatureTypeStyle>
        </UserStyle>
    </NamedLayer>
</StyledLayerDescriptor>
