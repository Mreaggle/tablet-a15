const {readFileSync, writeFileSync} = require('node:fs');
const {buildSync, transformSync} = require('esbuild');
const {Packager, loadProject} = require('@turbowarp/packager');
const acorn = require('acorn');

async function main() {
    const packager = new Packager();
    packager.project = await loadProject(readFileSync('hair-salon-sem-audio.sb2'));
    Object.assign(packager.options, {
        autoplay: true,
        bakeExtensions: false,
        maxTextureDimension: 1024
    });
    packager.options.app.windowTitle = 'Salão';
    packager.options.app.packageName = 'salao-a15';
    packager.options.controls.greenFlag.enabled = true;
    packager.options.cloudVariables.mode = 'none';
    packager.options.appearance.background = '#172c32';
    packager.options.appearance.accent = '#32b495';
    const {data} = await packager.package();
    const html = Buffer.from(data).toString('utf8');
    const compiled = html.replace(/(<script(?:\s[^>]*)?>)([\s\S]*?)(<\/script\s*>)/gi, (match, start, code, end) => {
        if ((code.trim() === '') || /\btype="application\//.test(start)) return match;
        const output = transformSync(code, {target: 'chrome66', legalComments: 'inline'}).code;
        try { acorn.parse(output, {ecmaVersion: 2020}); } catch (error) { console.error(output.slice(error.pos - 120, error.pos + 170)); throw error; }
        return start + output.replace(/<\/script/gi, '<\\/script') + end;
    });
    const polyfills = buildSync({
        entryPoints: ['core-js/stable'],
        bundle: true,
        write: false,
        minify: true,
        target: 'chrome66',
        legalComments: 'inline'
    }).outputFiles[0].text;
    writeFileSync('web/polyfills.js', polyfills);
    writeFileSync('web/index.html', compiled.replace(/<head>/i, '<head><script src="/polyfills.js"></script>'));
}
main().catch((error) => {console.error(error); process.exitCode = 1;});
