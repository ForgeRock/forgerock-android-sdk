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
    <script src="https://cdn.forgerock.com/backstage/web-components/stable/backstage-web-components-loader.js"></script>
    <@resources/>
</head>
<body>
    <backstage-site-header active-item="docs" fluid="true" theme="dark" active="docs" searchcategory="docs" searchfamily="sdks" searchversion="latest"></backstage-site-header>
    <div id="container">
      <main class="main-col">
        <div class="main-grid">
          <div id="leftColumn">
            <div id="titlePanel">
              <@template_cmd name="pathToRoot">
                  <h2><a href="${pathToRoot}index.html">
                      <@template_cmd name="projectName">
                          <span>${projectName}</span>
                      </@template_cmd>
                  </a></h2>
              </@template_cmd>
              <div id="versionPanel">
                  <@version/>
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
     <backstage-site-footer></backstage-site-footer>
  </div>
  <link rel="import"
              href="https://cdn.forgerock.com/backstage-web-components/latest/backstage-site-header.html"/>
  <link rel="import"
      href="https://cdn.forgerock.com/backstage-web-components/latest/backstage-site-footer.html"/>
</body>
</html>