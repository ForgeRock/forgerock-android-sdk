<#-- Copyright 2023 ForgeRock AS. All Rights Reserved. -->

<#import "page_metadata.ftl" as page_metadata>
<!DOCTYPE html>
<html>
<head>
    <meta name="viewport" content="width=device-width, initial-scale=1" charset="UTF-8">
    <@page_metadata.display/>
    <@template_cmd name="pathToRoot">
      <script>var pathToRoot = "${pathToRoot}";</script>
    </@template_cmd>
    <link rel="stylesheet" href="https://cdn.forgerock.com/docs-ui/adoc/dev-ping/vendor.antora-styles.css">
    <link rel="stylesheet" href="https://cdn.forgerock.com/docs-ui/adoc/dev-ping/ping.antora-styles.css">
    <@resources/>
</head>
<body>
	<header id="shared-navbar">
	  <div id="ping-navbar"></div>
	</header>
    <div id="container" class="body">
      <main class="main-col">
        <div class="main-grid">
          <div id="leftColumn">
            <div id="titlePanel">
              <@template_cmd name="pathToRoot">
                  <h3 class="title"><a href="${pathToRoot}index.html">
                      <@template_cmd name="projectName">
                          <span>${projectName}</span>
                      </@template_cmd>
                  </a></h3>
              </@template_cmd>
              <div class="page-versions toc-versions toc-version-dropdown">
                <button class="version-menu-toggle btn dropdown-toggle btn-outline-secondary btn-block text-dark"><@version/></button>
              </div>
            </div>
            <div id="sideMenu"></div>
          </div>
          <div id="content-container">
              <@content/>
              <div id="searchBar"></div>
          </div>
        </div>
     </main>
  </div>
<footer>
  <div id="webdev_footer_3rd_party"
      data-config-path="https://download.pingidentity.com/public/json-rework-nav-foot/stage/footers/www_en_reimagineFooterConfigs.json">
    {{!-- default content to be replaced by JavaScript footer --}}
    <p class="copyright">&copy; {{year}} <a href="https://pingidentity.com">Ping Identity Corporation</a>. All rights reserved.</p>
    <script
      defer type="module"
      src="https://www.pingidentity.com/api_cached/footer/latest/init"></script>
  </div>
</footer>

 <script src="https://cdn.forgerock.com/docs-ui/adoc/dev-ping/vendor.antora-scripts.js"></script>
 <script id="site-script" src="https://cdn.forgerock.com/docs-ui/adoc/dev-ping/ping.antora-scripts.js" data-sync-storage-key="frPersistTabs" data-sync-storage-scope="local"></script>


</body>
</html>