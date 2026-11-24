package org.tableta15.icones;

final class Targets {
    enum App {
        YOUTUBE("youtube", "Vídeos", "org.smarttube.phone", R.drawable.play_video),
        CHOGAN("chogan", "Chogan", "org.tableta15.chogan", R.drawable.joystick),
        GCOMPRIS("gcompris", "GCompris", "net.gcompris.full", R.drawable.puzzle),
        COLORBOOK("colorbook", "Colorbook", "com.sidhant.colorbook", R.drawable.paleta),
        DOLLMAKER("dollmaker", "DollMaker", "org.tableta15.dollmaker", R.drawable.boneca);

        final String id;
        final String label;
        final String packageName;
        final int icon;

        App(String id, String label, String packageName, int icon) {
            this.id = id;
            this.label = label;
            this.packageName = packageName;
            this.icon = icon;
        }

        static App find(String id) {
            for (App app : values()) {
                if (app.id.equals(id)) return app;
            }
            return null;
        }
    }

    static final String EXTRA_ICON = "icon";

    private Targets() {}
}
