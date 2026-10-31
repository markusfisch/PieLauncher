package de.markusfisch.android.pielauncher.widget.state;

public class ViewStateManager {

    public static final int MODE_PIE = 0;
    public static final int MODE_LIST = 1;
    public static final int MODE_EDIT = 2;

    public interface Listener {
        void invalidate();

        void setVerticalScrollBarEnabled(boolean enabled);

        void scrollList(int y, boolean isScrolling);

        void resetScrollWithoutAnimation();

        void fadeOutMode();

        void onModeChanged(int mode);
    }

    private final Fader fadePie = new Fader();
    private final Fader fadeList = new Fader();
    private final Fader fadeEdit = new Fader();

    private final Listener listener;
    private int mode = MODE_PIE;
    private float dragDistance;
    private float dragProgress;
    private int lastScrollY;
    private int listFadeHeight;

    public ViewStateManager(Listener listener) {
        this.listener = listener;
    }

    public int getMode() {
        return mode;
    }

    public boolean inListMode() {
        return mode == MODE_LIST;
    }

    public boolean inEditMode() {
        return mode == MODE_EDIT;
    }

    public Fader getFadePie() {
        return fadePie;
    }

    public Fader getFadeList() {
        return fadeList;
    }

    public Fader getFadeEdit() {
        return fadeEdit;
    }

    public float getDragProgress() {
        return dragProgress;
    }

    public int getLastScrollY() {
        return lastScrollY;
    }

    public void setLastScrollY(int y) {
        this.lastScrollY = y;
    }

    public void setListFadeHeight(int height) {
        this.listFadeHeight = height;
    }

    public void showList() {
        if (mode == MODE_LIST) {
            return;
        }
        mode = MODE_LIST;
        listener.onModeChanged(mode);
        listener.scrollList(lastScrollY, false);
        listener.setVerticalScrollBarEnabled(true);
        resetDragDownList();
        fadeList.fadeIn();
        listener.invalidate();
    }

    public void hideList() {
        if (mode == MODE_PIE) {
            return;
        }
        fadeList.setMaxIn(dragProgress);
        resetDragDownList();
        listener.fadeOutMode();
        mode = MODE_PIE;
        listener.onModeChanged(mode);
        listener.resetScrollWithoutAnimation();
        listener.setVerticalScrollBarEnabled(false);
        listener.invalidate();
    }

    public void showEditor() {
        if (mode == MODE_EDIT) {
            return;
        }
        mode = MODE_EDIT;
        listener.onModeChanged(mode);
        fadeEdit.fadeIn();
        listener.invalidate();
    }

    public void endEditMode() {
        listener.fadeOutMode();
        mode = MODE_PIE;
        listener.onModeChanged(mode);
        listener.invalidate();
    }

    public void resetDragDownList() {
        dragDistance = 0f;
        dragProgress = 1f;
    }

    public boolean isDraggingDownList() {
        return dragProgress < 1f;
    }

    public void dragDownListBy(float y) {
        if (mode != MODE_LIST || listFadeHeight <= 0) {
            return;
        }
        dragDistance -= y;
        dragProgress = Math.max(0f, Math.min(1f, 1f - (dragDistance / listFadeHeight)));
        listener.invalidate();
    }
}
