#!/bin/bash
# ============================================================
# CZN Wiki - OTA 一键发布脚本
# 功能：
#   1. 部署 admin 到 GitHub Pages (docs/index.html)
#   2. 同步 data JSON 到 app/src/main/assets/
#   3. 构建 Release APK
#   4. 复制 APK 到 output/
#   5. Git commit + push + tag
# ============================================================

set -euo pipefail

# ---- Configuration ----
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_DIR="$(dirname "$SCRIPT_DIR")"
ADMIN_DIR="${PROJECT_DIR}/admin"
DATA_DIR="${PROJECT_DIR}/data"
ASSETS_DIR="${PROJECT_DIR}/app/src/main/assets"
OUTPUT_DIR="${PROJECT_DIR}/output"

# ---- Colors ----
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m'

log_info()  { echo -e "${BLUE}[INFO]${NC}  $*"; }
log_ok()   { echo -e "${GREEN}[OK]${NC}    $*"; }
log_warn() { echo -e "${YELLOW}[WARN]${NC}  $*"; }
log_err()  { echo -e "${RED}[ERROR]${NC} $*"; }

# ---- Pre-flight checks ----
check_command() {
    if ! command -v "$1" &>/dev/null; then
        log_err "$1 not found. Please install it first."
        exit 1
    fi
}

preflight() {
    log_info "Running pre-flight checks..."
    check_command git
    check_command zip

    if [ ! -f "${PROJECT_DIR}/gradlew" ]; then
        log_err "gradlew not found in ${PROJECT_DIR}"
        exit 1
    fi

    if [ ! -d "${DATA_DIR}" ]; then
        log_err "data directory not found: ${DATA_DIR}"
        exit 1
    fi

    if [ ! -d "${ADMIN_DIR}" ]; then
        log_warn "admin directory not found, skipping admin deploy step"
    fi

    # Read version info
    VERSION_FILE="${PROJECT_DIR}/version.json"
    if [ ! -f "$VERSION_FILE" ]; then
        VERSION_FILE="${DATA_DIR}/version.json"
    fi
    if [ -f "$VERSION_FILE" ]; then
        VERSION=$(python3 -c "import json; print(json.load(open('$VERSION_FILE'))['version'])" 2>/dev/null || echo "unknown")
        log_info "Current version: ${VERSION}"
    else
        VERSION="unknown"
        log_warn "version.json not found, version set to 'unknown'"
    fi

    log_ok "Pre-flight checks passed"
}

# ---- Step 1: Deploy admin to GitHub Pages ----
deploy_admin_to_pages() {
    log_info "Step 1: Deploy admin to GitHub Pages"

    if [ ! -f "${ADMIN_DIR}/index.html" ]; then
        log_warn "${ADMIN_DIR}/index.html not found, skipping admin deploy"
        return 0
    fi

    # Copy admin to docs/ (GitHub Pages convention)
    DOCS_DIR="${PROJECT_DIR}/docs"
    mkdir -p "${DOCS_DIR}"
    cp "${ADMIN_DIR}/index.html" "${DOCS_DIR}/index.html"
    log_ok "Copied admin/index.html -> docs/index.html"

    # Stage for git
    cd "${PROJECT_DIR}"
    git add docs/index.html 2>/dev/null || log_warn "docs/index.html not tracked yet"
    log_ok "Admin staged for deployment"
}

# ---- Step 2: Sync data to assets ----
sync_data_to_assets() {
    log_info "Step 2: Sync data JSON to app assets"

    mkdir -p "${ASSETS_DIR}"

    local synced=0
    for json_file in "${DATA_DIR}"/*.json; do
        if [ -f "$json_file" ]; then
            local fname=$(basename "$json_file")
            cp "$json_file" "${ASSETS_DIR}/${fname}"
            log_info "  Synced: ${fname}"
            ((synced++))
        fi
    done

    if [ $synced -eq 0 ]; then
        log_warn "No JSON files found in ${DATA_DIR}"
    else
        log_ok "Synced ${synced} data file(s) to assets"
    fi

    cd "${PROJECT_DIR}"
    git add app/src/main/assets/*.json 2>/dev/null || true
}

# ---- Step 3: Build Release APK ----
build_release_apk() {
    log_info "Step 3: Build Release APK"

    cd "${PROJECT_DIR}"

    # Grant execute permission if needed
    chmod +x gradlew 2>/dev/null || true

    log_info "Running ./gradlew assembleRelease ..."
    ./gradlew assembleRelease --no-daemon --quiet 2>&1 | while IFS= read -r line; do
        if [[ "$line" == *"FAILED"* || "$line" == *"ERROR"* ]]; then
            log_err "$line"
        fi
    done

    APK_PATH="${PROJECT_DIR}/app/build/outputs/apk/release/app-release.apk"
    if [ -f "$APK_PATH" ]; then
        APK_SIZE=$(du -h "$APK_PATH" | cut -f1)
        log_ok "APK built successfully (${APK_SIZE}): ${APK_PATH}"
    else
        log_err "APK build failed: ${APK_PATH} not found"
        log_info "Checking for unsigned APK..."
        UNSIGNED_APK="${PROJECT_DIR}/app/build/outputs/apk/release/app-release-unsigned.apk"
        if [ -f "$UNSIGNED_APK" ]; then
            log_warn "Found unsigned APK: ${UNSIGNED_APK}"
            APK_PATH="$UNSIGNED_APK"
        else
            exit 1
        fi
    fi
}

# ---- Step 4: Copy APK to output ----
copy_apk_to_output() {
    log_info "Step 4: Copy APK to output/"

    mkdir -p "${OUTPUT_DIR}"

    local dest_name="czn-wiki-v${VERSION}-release.apk"
    cp "${APK_PATH}" "${OUTPUT_DIR}/${dest_name}"
    log_ok "Copied: ${dest_name} -> ${OUTPUT_DIR}/"

    # Also create a latest pointer
    cp "${APK_PATH}" "${OUTPUT_DIR}/czn-wiki-latest.apk"
    log_info "Created latest pointer: czn-wiki-latest.apk"
}

# ---- Step 5: Git commit + push + tag ----
git_commit_push_tag() {
    log_info "Step 5: Git commit + push + tag"

    cd "${PROJECT_DIR}"

    # Check if there are changes to commit
    if git diff --quiet && git diff --cached --quiet; then
        log_warn "No changes to commit"
    else
        local commit_msg="OTA: release v${VERSION} (versionCode auto)"
        git commit -m "$commit_msg"
        log_ok "Committed: $commit_msg"
    fi

    # Tag
    local tag_name="v${VERSION}"
    if git rev-parse "$tag_name" >/dev/null 2>&1; then
        log_warn "Tag ${tag_name} already exists, skipping"
    else
        git tag -a "$tag_name" -m "Release ${tag_name}"
        log_ok "Created tag: ${tag_name}"
    fi

    # Push
    log_info "Pushing to remote..."
    git push origin HEAD 2>&1 | tail -1
    git push origin "$tag_name" 2>&1 | tail -1
    log_ok "Pushed commits and tag ${tag_name}"
}

# ---- Main ----
main() {
    echo ""
    echo "============================================"
    echo "  CZN Wiki - OTA 一键发布"
    echo "============================================"
    echo ""

    preflight

    echo ""
    log_info "Starting publish pipeline for v${VERSION}..."
    echo ""

    deploy_admin_to_pages
    echo ""
    sync_data_to_assets
    echo ""
    build_release_apk
    echo ""
    copy_apk_to_output
    echo ""
    git_commit_push_tag

    echo ""
    echo "============================================"
    log_ok "Publish pipeline complete!"
    echo ""
    echo "  Version:  ${VERSION}"
    echo "  APK:      ${OUTPUT_DIR}/czn-wiki-v${VERSION}-release.apk"
    echo "  Admin:    https://nandy5474.github.io/czn-wiki-app/"
    echo ""
    echo "============================================"
}

main "$@"
