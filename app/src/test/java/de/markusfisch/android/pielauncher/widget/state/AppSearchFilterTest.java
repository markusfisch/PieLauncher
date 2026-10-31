package de.markusfisch.android.pielauncher.widget.state;

import android.content.Context;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.util.Collections;
import java.util.List;

import de.markusfisch.android.pielauncher.content.Apps;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 34)
public class AppSearchFilterTest {

    @Test
    public void getSelectedAppFromTrailingSpace() {
        assertEquals(-1, AppSearchFilter.getSelectedAppFromTrailingSpace(null));
        assertEquals(-1, AppSearchFilter.getSelectedAppFromTrailingSpace(""));
        assertEquals(0, AppSearchFilter.getSelectedAppFromTrailingSpace("a"));
        assertEquals(1, AppSearchFilter.getSelectedAppFromTrailingSpace("a "));
        assertEquals(2, AppSearchFilter.getSelectedAppFromTrailingSpace("a  "));

        // Edge case: string is entirely spaces
        assertEquals(0, AppSearchFilter.getSelectedAppFromTrailingSpace(" "));
        assertEquals(1, AppSearchFilter.getSelectedAppFromTrailingSpace("  "));
    }

    @Test
    public void storesQueryAndFilteredState() {
        AppSearchFilter.Listener mockListener = org.mockito.Mockito.mock(AppSearchFilter.Listener.class);
        AppSearchFilter filter = new AppSearchFilter(mockListener);

        assertEquals("", filter.getQuery());
        assertFalse(filter.isFiltered());

        Context context = androidx.test.core.app.ApplicationProvider.getApplicationContext();
        filter.filterAppList(context, "test", false);

        assertEquals("test", filter.getQuery());
        assertTrue(filter.isFiltered());

        filter.filterAppList(context, "", false);
        assertEquals("", filter.getQuery());
        assertFalse(filter.isFiltered());
    }
}
