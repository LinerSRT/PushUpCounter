package st.pushupcounter.view.util;

import android.Manifest;
import android.content.Context;
import android.widget.Toast;

import com.androidisland.ezpermission.EzPermission;

import java.util.Set;

/**
 * @author : "Line'R"
 * @mailto : serinity320@mail.com
 * @created : 22.02.2023, среда
 **/
public class PermissionWrapper {
    public static void requestPermissions(Context context, Callback callback, String... permissions){
        EzPermission.Companion.with(context)
                .permissions(permissions)
                .request((grantedPermissions, deniedPermissions, strings3) -> {
                    callback.onResult(grantedPermissions, deniedPermissions);
                    return null;
                });
    }
    public static void requestPermissions(Context context, Callback callback, String permission){
        EzPermission.Companion.with(context)
                .permissions(permission)
                .request((grantedPermissions, deniedPermissions, strings3) -> {
                    if(grantedPermissions.contains(permission)){
                        callback.onGranted();
                    } else{
                        callback.onDenied();
                    }
                    return null;
                });
    }

    public interface Callback{
        default void onResult(Set<String> grantedPermissions, Set<String> deniedPermissions){}
        default void onGranted(){}
        default void onDenied(){}
    }
}
