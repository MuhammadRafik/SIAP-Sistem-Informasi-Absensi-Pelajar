package com.nurulislam.siap.util;

import javafx.scene.Cursor;
import javafx.scene.Node;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.SeparatorMenuItem;
import javafx.scene.control.Tooltip;

/**
 * Menu popup pada avatar/inisial pengguna di kanan atas setiap halaman.
 * Berisi "Profil Saya" (buka halaman profil yang bisa diedit) dan "Logout".
 * Dipasang sekali dari initialize() tiap controller lewat
 * {@link #pasang(Node, Runnable, Runnable)}.
 */
public final class ProfileMenu {

    private ProfileMenu() {
    }

    /**
     * @param anchor     node avatar yang diklik (StackPane lingkaran inisial)
     * @param bukaProfil aksi membuka halaman Profil Saya (boleh null)
     * @param keluar     aksi logout (boleh null)
     */
    public static void pasang(Node anchor, Runnable bukaProfil, Runnable keluar) {
        if (anchor == null) {
            return;
        }
        anchor.setCursor(Cursor.HAND);
        Tooltip.install(anchor, new Tooltip("Klik untuk menu profil"));

        ContextMenu menu = new ContextMenu();
        MenuItem itemProfil = new MenuItem("Profil Saya");
        MenuItem itemKeluar = new MenuItem("Logout");
        itemProfil.setOnAction(e -> {
            menu.hide();
            if (bukaProfil != null) {
                bukaProfil.run();
            }
        });
        itemKeluar.setOnAction(e -> {
            menu.hide();
            if (keluar != null) {
                keluar.run();
            }
        });
        menu.getItems().addAll(itemProfil, new SeparatorMenuItem(), itemKeluar);

        anchor.setOnMouseClicked(e -> {
            if (menu.isShowing()) {
                menu.hide();
            } else {
                menu.show(anchor, e.getScreenX(), e.getScreenY());
            }
        });
    }
}
