<%@ page contentType="text/html; charset=UTF-8" isELIgnored="false" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>

<!DOCTYPE html>
<html>
<head>
    <title>Liiteri-tietopalvelu</title>
	<meta http-equiv="Content-Type" content="text/html; charset=utf-8"/>
    <script type="text/javascript" src="/Oskari/libraries/jquery/jquery-1.10.2.min.js">
    </script>


    <!-- ############# css ################# -->
	<link href='//fonts.googleapis.com/css?family=Open+Sans:400,300,700,600,400italic,600italic' rel='stylesheet' type='text/css' />

    <link
            rel="stylesheet"
            type="text/css"
            href="/Oskari/resources/css/forms.css"/>
    <link
            rel="stylesheet"
            type="text/css"
            href="/Oskari/resources/css/portal.css"/>
    <link
            rel="stylesheet"
            type="text/css"
            href="/Oskari${path}/icons.css"/>
    <link
            rel="stylesheet"
            type="text/css"
            href="/Oskari${path}/css/overwritten.css"/>

    <link
            rel="stylesheet"
            type="text/css"
            href="/Oskari${path}/css/bootstrap/bootstrap.css"/>

	<link
            rel="stylesheet"
            type="text/css"
            href="/Oskari${path}/css/bootstrap/bootstrap-theme.css"/>

	<script src="/Oskari/libraries/bootstrap/bootstrap.min.js"></script>

    <style type="text/css">
        @media screen {
            body {
                margin: 0;
                padding: 0;
            }

            #mapdiv {
                width: 100%;
            }

            #maptools {
                background-color: #0186d1;
                height: 100%;
                position: absolute;
                top: 0;
                width: 180px;
                z-index: 2;
            }

            #minimized_maptools {
				display: none;
				background-color: #00bce9;
                height: 100%;
                position: absolute;
                top: 0;
                width: 16px;
                z-index: 2;
			}

			#access_nav_show {
				margin-top: 5px;
				width: 15px;
				height: 8px;
				background-image: url("/Oskari/resources/show_view.png");
				display: block;
			}

			#access_nav_hide {
				margin-top: 5px;
				margin-right: 5px;
				height: 8px;
				background-image: url("/Oskari/resources/hide_view.png");
				display: block;
				background-repeat: no-repeat;
  				background-position: right;
			}

            #contentMap {
                height: 100%;
                margin-left: 175px;
            }

            #login {
                margin-left: 5px;
            }

            #login input[type="text"], #login input[type="password"] {
                width: 90%;
                margin-bottom: 5px;
                background-image: url("/Oskari${path}/images/forms/input_shadow.png");
                background-repeat: no-repeat;
                padding-left: 5px;
                padding-right: 5px;
                border: 1px solid #B7B7B7;
                border-radius: 4px 4px 4px 4px;
                box-shadow: 0 1px 2px rgba(0, 0, 0, 0.1) inset;
                color: #878787;
                font: 13px/100% Arial,sans-serif;
            }
            #login input[type="submit"] {
                width: 90%;
                margin-bottom: 5px;
                padding-left: 5px;
                padding-right: 5px;
                border: 1px solid #B7B7B7;
                border-radius: 4px 4px 4px 4px;
                box-shadow: 0 1px 2px rgba(0, 0, 0, 0.1) inset;
                color: #878787;
                font: 13px/100% Arial,sans-serif;
            }
            #login p.error {
                font-weight: bold;
                color : red;
                margin-bottom: 10px;
            }

            #login a {
                color: #FFF;
                padding: 5px;
            }

			.oskari-tile-closed {
				background-color: #0186d1;
			}
        }
    </style>
    <!-- ############# /css ################# -->
</head>
<body>

<nav id="minimized_maptools">
	<a id="access_nav_show" href="#"></a>
</nav>

<nav id="maptools">
	<div id="maptools-container">
		<div>
			<a id="access_nav_hide" href="#"></a>
		</div>
		<div id="logo">
			<a href="${baseUrl}">
				<img src="/Oskari/resources/2014_Liiteri_logo_153x55px.png" alt="Liiteri logo" />
			</a>
		</div>
		<div id="loginbar">
		</div>
		<div id="menubar">
		</div>
		<div id="divider">
		</div>
		<div id="footer-nav">
			<div id="toolbar">
			</div>
		</div>
	</div>
</nav>
<div id="contentMap" class="oskariui container-fluid">
    <div id="menutoolbar" class="container-fluid"></div>
    <div class="row-fluid oskariui-mode-content" style="height: 100%; background-color:white;">
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
    var language = '${language}';
    var preloaded = ${preloaded};
    var controlParams = ${controlParams};
</script>

<script type="text/javascript"
        src="/Oskari/bundles/bundle.js">
</script>

<c:if test="${preloaded}">

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
            src="/Oskari${path}/oskari_lang_${language}.js">
    </script>
</c:if>

<script type="text/javascript"
        src="/Oskari${path}/index.js">
</script>

<!-- ############# /Javascript ################# -->
</body>
</html>
