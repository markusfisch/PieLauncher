package de.markusfisch.android.pielauncher.widget.state;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@org.junit.runner.RunWith(org.robolectric.RobolectricTestRunner.class)
@org.robolectric.annotation.Config(sdk = 34)
public class ViewStateManagerTest {

    @Mock
    private ViewStateManager.Listener mockListener;

    private ViewStateManager viewStateManager;

    @Before
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        viewStateManager = new ViewStateManager(mockListener);
        viewStateManager.setListFadeHeight(100);
    }

    @Test
    public void testInitialState_IsPieMode() {
        assertEquals(ViewStateManager.MODE_PIE, viewStateManager.getMode());
        assertFalse(viewStateManager.inListMode());
        assertFalse(viewStateManager.inEditMode());
    }

    @Test
    public void testShowList_ChangesModeAndNotifiesListener() {
        viewStateManager.setLastScrollY(50);
        viewStateManager.showList();

        assertEquals(ViewStateManager.MODE_LIST, viewStateManager.getMode());
        assertTrue(viewStateManager.inListMode());
        verify(mockListener, times(1)).scrollList(eq(50), eq(false));
        verify(mockListener, times(1)).setVerticalScrollBarEnabled(eq(true));
        verify(mockListener, times(1)).invalidate();
    }

    @Test
    public void testHideList_ChangesModeAndFadesOut() {
        viewStateManager.showList(); // Start in list mode
        viewStateManager.hideList();

        assertEquals(ViewStateManager.MODE_PIE, viewStateManager.getMode());
        verify(mockListener, times(1)).fadeOutMode();
        verify(mockListener, times(1)).resetScrollWithoutAnimation();
        verify(mockListener, times(1)).setVerticalScrollBarEnabled(eq(false));
        verify(mockListener, times(2)).invalidate(); // 1 from showList, 1 from hideList
    }

    @Test
    public void testShowEditor_ChangesModeAndInvalidates() {
        viewStateManager.showEditor();

        assertEquals(ViewStateManager.MODE_EDIT, viewStateManager.getMode());
        assertTrue(viewStateManager.inEditMode());
        verify(mockListener, times(1)).invalidate();
    }

    @Test
    public void testEndEditMode_ChangesToPieModeAndFadesOut() {
        viewStateManager.showEditor(); // Start in edit mode
        viewStateManager.endEditMode();

        assertEquals(ViewStateManager.MODE_PIE, viewStateManager.getMode());
        verify(mockListener, times(1)).fadeOutMode();
        verify(mockListener, times(2)).invalidate(); // 1 from showEditor, 1 from endEditMode
    }

    @Test
    public void testDragDownListBy_UpdatesDragProgress() {
        viewStateManager.showList();

        viewStateManager.dragDownListBy(50f);

        // Negative delta simulates dragging list downwards, increasing drag distance
        viewStateManager.resetDragDownList();
        viewStateManager.dragDownListBy(-50f);

        // dragDistance = 50. progress = 1 - (50/100) = 0.5
        assertEquals(0.5f, viewStateManager.getDragProgress(), 0.001f);
        assertTrue(viewStateManager.isDraggingDownList());
    }
}
