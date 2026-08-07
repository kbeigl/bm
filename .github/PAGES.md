# GitHub Pages Deployment

This document explains how the documentation is automatically published to GitHub Pages.

## Overview

The documentation is automatically built and deployed to https://kbeigl.github.io/bm/ whenever code is pushed to the `main` branch.

## Workflow

The deployment is handled by `.github/workflows/pages.yml` which:

1. **Builds the project** - Compiles all modules with Maven
2. **Starts Kroki Server** - Launches a Docker container for diagram generation
3. **Generates documentation** - Runs asciidoctor-maven-plugin to convert AsciiDoc to HTML
4. **Deploys to GitHub Pages** - Uploads the generated HTML to GitHub Pages

## Setup Requirements

### 1. Enable GitHub Pages

In your GitHub repository settings:

1. Go to **Settings** → **Pages**
2. Under **Source**, select:
   - Source: **GitHub Actions** (not "Deploy from a branch")
3. Save the settings

### 2. Verify Permissions

The workflow requires these permissions (already configured in `pages.yml`):
- `contents: read` - To checkout the code
- `pages: write` - To deploy to GitHub Pages
- `id-token: write` - For GitHub Pages authentication

## Workflow Triggers

The workflow runs automatically when:
- Code is pushed to the `main` branch
- Manually triggered from the Actions tab

## Local Testing

To test the documentation build locally:

```bash
# Start Kroki server (optional, if you have diagrams)
docker run -d --name kroki -p 8002:8000 yuzutech/kroki

# Build the documentation
cd bm-docs
../mvnw clean process-resources

# View the generated HTML
open target/generated-docs/bm.html
```

## File Structure

After deployment, the documentation is available at:

```
https://kbeigl.github.io/bm/
├── index.html                    # Redirects to bm.html
├── bm.html                       # Main documentation
├── releases.html
├── bm-docs/
├── bm-parent/
│   ├── bm-parent.html
│   ├── maven-maintenance.html
│   └── ...
├── bm-tracker/
│   ├── gps-osmand-player/
│   └── gps-osmand-tracker/
└── bm-traccar/
    ├── traccar-api-client/
    ├── traccar-openapitools-client/
    └── traccar-realtime-client/
```

## Navigation

All cross-references (xref) in the AsciiDoc files are converted to working HTML links, maintaining the same directory structure and navigation patterns.

## Troubleshooting

### Documentation not updating

1. Check the Actions tab for workflow status
2. Look for errors in the build logs
3. Ensure GitHub Pages is enabled with "GitHub Actions" as source

### Diagrams not rendering

The workflow uses a Kroki Docker container. If diagrams fail:
- Check Docker service is available in GitHub Actions
- Verify Kroki server startup logs
- Consider using public kroki.io as fallback

### Build fails

Common issues:
- Maven build errors: Check that all modules compile successfully
- Include directive errors: Ensure `relativeBaseDir: true` in asciidoctor config
- Missing files: Verify all adoc files are copied to `combined-asciidoc`

## Manual Deployment

To manually trigger deployment:

1. Go to **Actions** tab in GitHub
2. Select **Deploy Documentation to GitHub Pages**
3. Click **Run workflow**
4. Select branch `main` and click **Run workflow**

## Monitoring

View deployment status:
- **Actions tab**: Shows workflow runs and logs
- **Deployments**: Shows GitHub Pages deployment history
- **Settings → Pages**: Shows current deployment URL
