<%@ page contentType="text/html; charset=UTF-8" isELIgnored="false" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<!DOCTYPE html>
<html>
<head>
    <title>Standalone servlet - ${viewName} view</title>

    <script type="text/javascript" src="/Oskari/libraries/jquery/jquery-1.7.1.min.js">
    </script>

    <!-- ############# css ################# -->
    <link
            rel="stylesheet"
            type="text/css"
            href="/Oskari${path}/css/icons.css"/>
    <link
            rel="stylesheet"
            type="text/css"
            href="/Oskari${path}/css/forms.css"/>
    <link
            rel="stylesheet"
            type="text/css"
            href="/Oskari${path}/css/portal.css"/>

    <link
            rel="stylesheet"
            type="text/css"
            href="/Oskari${path}/css/overwritten.css"/>
    <style type="text/css">
        @media screen {
            body {
                margin : 0;
                padding : 0;
            }
            #mapdiv {
                width: 100%;
            }
            #contentMap {
                height: 100%;
            }
        }
    </style>
    <!-- ############# /css ################# -->
</head>
<body>
<div id="contentMap" class="oskariui container-fluid published">
    <div class="row-fluid" style="height: 100%; background-color:white;">
        <div class="oskariui-left"></div>
        <div class="span12 oskariui-center" style="height: 100%; margin: 0;">
            <div id="mapdiv"></div>
        </div>
        <div class="oskari-closed oskariui-right">
            <div id="mapdivB"></div>
        </div>
    </div>
</div>


<!-- ############# Javascript ################# -->

<!--  OSKARI -->

<script type="text/javascript">
    var ajaxUrl = '${ajaxUrl}';
    var viewId = '${viewId}';
    var ssl = false;
    var language = '${language}';
    var preloaded = ${preloaded};
    var controlParams = ${controlParams};
</script>

<script type="text/javascript"
        src="/Oskari/bundles/bundle.js">
</script>

<!--  OPENLAYERS -->
<script type="text/javascript"
        src="/Oskari/packages/openlayers/startup.js">
</script>

<c:if test="${preloaded}">
    <!-- Pre-compiled application JS, empty unless created by build job -->
    <script type="text/javascript"
            src="/Oskari${path}/oskari.min.js">
    </script>
    <!-- Minified CSS for preload -->
    <link
            rel="stylesheet"
            type="text/css"
            href="/Oskari${path}/oskari.min.css"
            />
    <%--language files --%>
    <script type="text/javascript"
            src="/Oskari${path}/oskari_lang_all.js">
    </script>
    <script type="text/javascript"
            src="/Oskari${path}/oskari_lang_${language}.js">
    </script>
</c:if>

<script type="text/javascript"
        src="/Oskari${path}/index.js">
</script>


<!-- ############# /Javascript ################# -->
</body>
</html>
