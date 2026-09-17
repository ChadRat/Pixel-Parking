/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  android.app.Application
 *  android.content.Context
 *  android.content.Intent
 *  android.content.SharedPreferences
 *  android.location.Location
 *  android.net.Uri
 *  android.os.Build$VERSION
 *  android.os.VibrationEffect
 *  android.widget.Toast
 *  androidx.compose.runtime.internal.StabilityInferred
 *  androidx.lifecycle.AndroidViewModel
 *  androidx.lifecycle.ViewModel
 *  androidx.lifecycle.ViewModelKt
 *  com.google.android.gms.location.LocationCallback
 *  kotlin.Metadata
 *  kotlin.Pair
 *  kotlin.ResultKt
 *  kotlin.Unit
 *  kotlin.collections.CollectionsKt
 *  kotlin.collections.MapsKt
 *  kotlin.collections.SetsKt
 *  kotlin.comparisons.ComparisonsKt
 *  kotlin.coroutines.Continuation
 *  kotlin.coroutines.CoroutineContext
 *  kotlin.coroutines.intrinsics.IntrinsicsKt
 *  kotlin.coroutines.jvm.internal.Boxing
 *  kotlin.coroutines.jvm.internal.SpillingKt
 *  kotlin.jvm.functions.Function1
 *  kotlin.jvm.functions.Function2
 *  kotlin.jvm.functions.Function4
 *  kotlin.jvm.internal.Intrinsics
 *  kotlin.jvm.internal.SourceDebugExtension
 *  kotlin.random.Random
 *  kotlin.ranges.RangesKt
 *  kotlin.text.StringsKt
 *  kotlinx.coroutines.BuildersKt
 *  kotlinx.coroutines.CoroutineScope
 *  kotlinx.coroutines.CoroutineScopeKt
 *  kotlinx.coroutines.DelayKt
 *  kotlinx.coroutines.Dispatchers
 *  kotlinx.coroutines.Job
 *  kotlinx.coroutines.Job$DefaultImpls
 *  kotlinx.coroutines.flow.Flow
 *  kotlinx.coroutines.flow.FlowKt
 *  kotlinx.coroutines.flow.MutableStateFlow
 *  kotlinx.coroutines.flow.SharingStarted
 *  kotlinx.coroutines.flow.SharingStarted$Companion
 *  kotlinx.coroutines.flow.StateFlow
 *  kotlinx.coroutines.flow.StateFlowKt
 *  org.jetbrains.annotations.NotNull
 *  org.jetbrains.annotations.Nullable
 */
package com.example.ui.viewmodel;

import android.app.Application;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.location.Location;
import android.net.Uri;
import android.os.Build;
import android.os.VibrationEffect;
import android.widget.Toast;
import androidx.compose.runtime.internal.StabilityInferred;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelKt;
import com.example.AutoParkApplication;
import com.example.data.entity.BluetoothCarDevice;
import com.example.data.entity.ParkingSpot;
import com.example.data.repository.ParkingRepository;
import com.example.notification.ParkingNotificationHelper;
import com.example.sensor.AdaptiveWaypointHapticScheduler;
import com.example.sensor.CompassSensorManager;
import com.example.sensor.CompassState;
import com.example.sensor.DeviceHardwareProfile;
import com.example.sensor.HapticPatternEvent;
import com.example.sensor.HardwareSensorProfiler;
import com.example.sensor.LocationHelper;
import com.example.sensor.SunCalculator;
import com.example.sensor.WaypointHapticMode;
import com.example.service.ParkingRadarService;
import com.example.service.WaypointHapticService;
import com.example.ui.components.CarBadgeStyle;
import com.example.ui.i18n.AppLanguage;
import com.example.ui.theme.AppThemeMode;
import com.example.ui.viewmodel.AppTab;
import com.example.ui.viewmodel.NavigationTelemetry;
import com.example.util.AlarmSoundHelper;
import com.example.util.BluetoothDeviceHelper;
import com.example.util.HapticHelper;
import com.example.util.HapticProfile;
import com.google.android.gms.location.LocationCallback;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import kotlin.Metadata;
import kotlin.Pair;
import kotlin.ResultKt;
import kotlin.Unit;
import kotlin.collections.CollectionsKt;
import kotlin.collections.MapsKt;
import kotlin.collections.SetsKt;
import kotlin.comparisons.ComparisonsKt;
import kotlin.coroutines.Continuation;
import kotlin.coroutines.CoroutineContext;
import kotlin.coroutines.intrinsics.IntrinsicsKt;
import kotlin.coroutines.jvm.internal.Boxing;
import kotlin.coroutines.jvm.internal.SpillingKt;
import kotlin.jvm.functions.Function1;
import kotlin.jvm.functions.Function2;
import kotlin.jvm.functions.Function4;
import kotlin.jvm.internal.Intrinsics;
import kotlin.jvm.internal.SourceDebugExtension;
import kotlin.random.Random;
import kotlin.ranges.RangesKt;
import kotlin.text.StringsKt;
import kotlinx.coroutines.BuildersKt;
import kotlinx.coroutines.CoroutineScope;
import kotlinx.coroutines.CoroutineScopeKt;
import kotlinx.coroutines.DelayKt;
import kotlinx.coroutines.Dispatchers;
import kotlinx.coroutines.Job;
import kotlinx.coroutines.flow.Flow;
import kotlinx.coroutines.flow.FlowKt;
import kotlinx.coroutines.flow.MutableStateFlow;
import kotlinx.coroutines.flow.SharingStarted;
import kotlinx.coroutines.flow.StateFlow;
import kotlinx.coroutines.flow.StateFlowKt;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@Metadata(mv={2, 2, 0}, k=1, xi=48, d1={"\u0000\u0092\u0002\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0003\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0003\n\u0002\u0010\u000b\n\u0002\b\u000b\n\u0002\u0018\u0002\n\u0002\b\u0003\n\u0002\u0018\u0002\n\u0002\b\u0003\n\u0002\u0018\u0002\n\u0002\b\u0003\n\u0002\u0018\u0002\n\u0002\b\u0003\n\u0002\u0018\u0002\n\u0002\b\u0003\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0010 \n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0002\b\u0003\n\u0002\u0010\u000e\n\u0002\b\u0003\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0014\n\u0002\u0010\b\n\u0002\b\u0003\n\u0002\u0010\u0007\n\u0002\b\u0003\n\u0002\u0010\t\n\u0002\b\u000f\n\u0002\u0010\"\n\u0002\b\u000b\n\u0002\u0018\u0002\n\u0002\b\u0005\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0010\u0002\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0002\u0010\u0006\n\u0002\b\u0016\n\u0002\u0018\u0002\n\u0002\b\u0004\n\u0002\u0010!\n\u0002\b\r\n\u0002\u0018\u0002\n\u0002\b&\n\u0002\u0018\u0002\n\u0002\b$\n\u0002\u0018\u0002\n\u0002\b\u0003\b\u0007\u0018\u00002\u00020\u0001B\u000f\u0012\u0006\u0010\u0002\u001a\u00020\u0003\u00a2\u0006\u0004\b\u0004\u0010\u0005J\b\u0010\u008c\u0001\u001a\u00030\u008d\u0001J\b\u0010\u008e\u0001\u001a\u00030\u008d\u0001J*\u0010\u008f\u0001\u001a\u0013\u0012\u0005\u0012\u00030\u0091\u0001\u0012\u0005\u0012\u00030\u0091\u0001\u0018\u00010\u0090\u00012\u0007\u0010\u0092\u0001\u001a\u00020EH\u0086@\u00a2\u0006\u0003\u0010\u0093\u0001J$\u0010\u0094\u0001\u001a\u00020E2\b\u0010\u0095\u0001\u001a\u00030\u0091\u00012\b\u0010\u0096\u0001\u001a\u00030\u0091\u0001H\u0086@\u00a2\u0006\u0003\u0010\u0097\u0001J\u0011\u0010\u0098\u0001\u001a\u00030\u008d\u00012\u0007\u0010\u0099\u0001\u001a\u00020\u000fJ\u0011\u0010\u009a\u0001\u001a\u00030\u008d\u00012\u0007\u0010\u009b\u0001\u001a\u00020\u0015J\b\u0010\u009c\u0001\u001a\u00030\u008d\u0001J\u0011\u0010\u009d\u0001\u001a\u00030\u008d\u00012\u0007\u0010\u009b\u0001\u001a\u00020\u0015J\u0011\u0010\u009e\u0001\u001a\u00030\u008d\u00012\u0007\u0010\u009b\u0001\u001a\u00020\u0015J\u0011\u0010\u009f\u0001\u001a\u00030\u008d\u00012\u0007\u0010\u00a0\u0001\u001a\u00020!J\u0011\u0010\u00a1\u0001\u001a\u00030\u008d\u00012\u0007\u0010\u00a2\u0001\u001a\u00020%J\u0011\u0010\u00a3\u0001\u001a\u00030\u008d\u00012\u0007\u0010\u00a4\u0001\u001a\u00020)J\u0011\u0010\u00a5\u0001\u001a\u00030\u008d\u00012\u0007\u0010\u0099\u0001\u001a\u000201J\u0012\u0010\u00a6\u0001\u001a\u00030\u008d\u00012\b\u0010\u00a7\u0001\u001a\u00030\u00a8\u0001J\u0011\u0010\u00a9\u0001\u001a\u00030\u008d\u00012\u0007\u0010\u00aa\u0001\u001a\u00020EJ\b\u0010\u00ab\u0001\u001a\u00030\u008d\u0001J\u001c\u0010\u00ae\u0001\u001a\u00030\u008d\u00012\u0007\u0010\u00af\u0001\u001a\u00020K2\t\b\u0002\u0010\u00b0\u0001\u001a\u00020\u0015J\u0007\u0010\u00b1\u0001\u001a\u00020\u0015J\b\u0010\u00b5\u0001\u001a\u00030\u008d\u0001J\b\u0010\u00b6\u0001\u001a\u00030\u008d\u0001J\b\u0010\u00b7\u0001\u001a\u00030\u008d\u0001J\b\u0010\u00b8\u0001\u001a\u00030\u008d\u0001J%\u0010\u00b9\u0001\u001a\u00030\u008d\u00012\u001b\b\u0002\u0010\u00ba\u0001\u001a\u0014\u0012\u0006\u0012\u0004\u0018\u00010A\u0012\u0005\u0012\u00030\u008d\u0001\u0018\u00010\u00bb\u0001JX\u0010\u00bc\u0001\u001a\u00030\u008d\u00012\t\b\u0002\u0010\u00aa\u0001\u001a\u00020E2\t\b\u0002\u0010\u00bd\u0001\u001a\u00020E2\t\b\u0002\u0010\u00be\u0001\u001a\u00020E2\f\b\u0002\u0010\u00bf\u0001\u001a\u0005\u0018\u00010\u0091\u00012\f\b\u0002\u0010\u00c0\u0001\u001a\u0005\u0018\u00010\u0091\u00012\u000b\b\u0002\u0010\u00c1\u0001\u001a\u0004\u0018\u00010E\u00a2\u0006\u0003\u0010\u00c2\u0001J?\u0010\u00c3\u0001\u001a\u00030\u008d\u00012\u0007\u0010\u00c4\u0001\u001a\u00020h2\u0007\u0010\u00c5\u0001\u001a\u00020E2\u0007\u0010\u00c6\u0001\u001a\u00020E2\u0007\u0010\u00c7\u0001\u001a\u00020E2\u000b\b\u0002\u0010\u00c8\u0001\u001a\u0004\u0018\u00010`\u00a2\u0006\u0003\u0010\u00c9\u0001J\u001a\u0010\u00ca\u0001\u001a\u00030\u008d\u00012\u0007\u0010\u00cb\u0001\u001a\u00020h2\u0007\u0010\u00c7\u0001\u001a\u00020EJ[\u0010\u00cc\u0001\u001a\u00030\u008d\u00012\u0007\u0010\u00c4\u0001\u001a\u00020h2\u0007\u0010\u00cd\u0001\u001a\u00020E2\u0007\u0010\u00ce\u0001\u001a\u00020E2\u0007\u0010\u00be\u0001\u001a\u00020E2\f\b\u0002\u0010\u00bf\u0001\u001a\u0005\u0018\u00010\u0091\u00012\f\b\u0002\u0010\u00c0\u0001\u001a\u0005\u0018\u00010\u0091\u00012\u000b\b\u0002\u0010\u00c1\u0001\u001a\u0004\u0018\u00010E\u00a2\u0006\u0003\u0010\u00cf\u0001J\u0011\u0010\u00d0\u0001\u001a\u00030\u008d\u00012\u0007\u0010\u00d1\u0001\u001a\u000205J\u0011\u0010\u00d2\u0001\u001a\u00030\u008d\u00012\u0007\u0010\u00d3\u0001\u001a\u00020`J\b\u0010\u00d4\u0001\u001a\u00030\u008d\u0001J\u0011\u0010\u00d5\u0001\u001a\u00030\u008d\u00012\u0007\u0010\u00c4\u0001\u001a\u00020hJ\b\u0010\u00d6\u0001\u001a\u00030\u008d\u0001J\b\u0010\u00d7\u0001\u001a\u00030\u008d\u0001J\u0011\u0010\u00d8\u0001\u001a\u00030\u008d\u00012\u0007\u0010\u00d9\u0001\u001a\u00020EJ\u001a\u0010\u00da\u0001\u001a\u00030\u008d\u00012\u0007\u0010\u00d9\u0001\u001a\u00020E2\u0007\u0010\u00db\u0001\u001a\u00020\u0015J\u001a\u0010\u00dc\u0001\u001a\u00030\u008d\u00012\u0007\u0010\u00d9\u0001\u001a\u00020E2\u0007\u0010\u00c5\u0001\u001a\u00020EJ\u0011\u0010\u00dd\u0001\u001a\u00030\u008d\u00012\u0007\u0010\u00d9\u0001\u001a\u00020EJ#\u0010\u00de\u0001\u001a\u00030\u008d\u00012\u0007\u0010\u00cd\u0001\u001a\u00020E2\u0007\u0010\u00d9\u0001\u001a\u00020E2\u0007\u0010\u00df\u0001\u001a\u00020EJ\u0012\u0010\u00e0\u0001\u001a\u00030\u008d\u00012\b\u0010\u00e1\u0001\u001a\u00030\u00e2\u0001J\u001a\u0010\u00e3\u0001\u001a\u00030\u008d\u00012\u0007\u0010\u00e4\u0001\u001a\u00020E2\u0007\u0010\u00e5\u0001\u001a\u00020EJ\u001f\u0010\u00e6\u0001\u001a\u00030\u008d\u00012\b\u0010\u00e1\u0001\u001a\u00030\u00e2\u00012\u000b\b\u0002\u0010\u00d1\u0001\u001a\u0004\u0018\u000105J\u001f\u0010\u00e7\u0001\u001a\u00030\u008d\u00012\b\u0010\u00e1\u0001\u001a\u00030\u00e2\u00012\u000b\b\u0002\u0010\u00d1\u0001\u001a\u0004\u0018\u000105J\b\u0010\u00e8\u0001\u001a\u00030\u008d\u0001J\b\u0010\u00e9\u0001\u001a\u00030\u008d\u0001J\b\u0010\u00ea\u0001\u001a\u00030\u008d\u0001J\u0011\u0010\u00eb\u0001\u001a\u00030\u008d\u00012\u0007\u0010\u009b\u0001\u001a\u00020\u0015J\u0011\u0010\u00ec\u0001\u001a\u00030\u008d\u00012\u0007\u0010\u009b\u0001\u001a\u00020\u0015J\u0011\u0010\u00ed\u0001\u001a\u00030\u008d\u00012\u0007\u0010\u009b\u0001\u001a\u00020\u0015J\u0011\u0010\u00ee\u0001\u001a\u00030\u008d\u00012\u0007\u0010\u009b\u0001\u001a\u00020\u0015J\u0010\u0010\u00ef\u0001\u001a\u00020\u00152\u0007\u0010\u00f0\u0001\u001a\u00020;J\b\u0010\u00f1\u0001\u001a\u00030\u008d\u0001J\b\u0010\u00f2\u0001\u001a\u00030\u008d\u0001J\b\u0010\u00f3\u0001\u001a\u00030\u008d\u0001J\b\u0010\u00f4\u0001\u001a\u00030\u008d\u0001J\b\u0010\u00f5\u0001\u001a\u00030\u008d\u0001J\b\u0010\u00f6\u0001\u001a\u00030\u008d\u0001J\b\u0010\u00f7\u0001\u001a\u00030\u008d\u0001J\b\u0010\u00f8\u0001\u001a\u00030\u008d\u0001J\u0011\u0010\u00f9\u0001\u001a\u00030\u008d\u00012\u0007\u0010\u00d3\u0001\u001a\u00020`J\u0011\u0010\u00fa\u0001\u001a\u00030\u008d\u00012\u0007\u0010\u00fb\u0001\u001a\u00020hJ\u0011\u0010\u00fc\u0001\u001a\u00030\u008d\u00012\u0007\u0010\u00fd\u0001\u001a\u00020\u0015J\u001a\u0010\u00fe\u0001\u001a\u00030\u008d\u00012\u0007\u0010\u00ff\u0001\u001a\u00020`2\u0007\u0010\u0080\u0002\u001a\u00020\u0015J\u0017\u0010\u0081\u0002\u001a\u00030\u008d\u00012\r\u0010\u0082\u0002\u001a\b\u0012\u0004\u0012\u00020`08J\b\u0010\u0083\u0002\u001a\u00030\u008d\u0001J\u0013\u0010\u0084\u0002\u001a\u00030\u008d\u00012\t\b\u0002\u0010\u00d3\u0001\u001a\u00020`J\u0012\u0010\u0085\u0002\u001a\u00030\u008d\u00012\b\u0010\u0086\u0002\u001a\u00030\u0087\u0002J\b\u0010\u0088\u0002\u001a\u00030\u008d\u0001J\n\u0010\u0089\u0002\u001a\u00030\u008d\u0001H\u0014R\u000e\u0010\u0006\u001a\u00020\u0007X\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u000e\u0010\b\u001a\u00020\tX\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u0016\u0010\n\u001a\n \f*\u0004\u0018\u00010\u000b0\u000bX\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u0014\u0010\r\u001a\b\u0012\u0004\u0012\u00020\u000f0\u000eX\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u0017\u0010\u0010\u001a\b\u0012\u0004\u0012\u00020\u000f0\u0011\u00a2\u0006\b\n\u0000\u001a\u0004\b\u0012\u0010\u0013R\u0014\u0010\u0014\u001a\b\u0012\u0004\u0012\u00020\u00150\u000eX\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u0017\u0010\u0016\u001a\b\u0012\u0004\u0012\u00020\u00150\u0011\u00a2\u0006\b\n\u0000\u001a\u0004\b\u0017\u0010\u0013R\u0014\u0010\u0018\u001a\b\u0012\u0004\u0012\u00020\u00150\u000eX\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u0017\u0010\u0019\u001a\b\u0012\u0004\u0012\u00020\u00150\u0011\u00a2\u0006\b\n\u0000\u001a\u0004\b\u001a\u0010\u0013R\u0014\u0010\u001b\u001a\b\u0012\u0004\u0012\u00020\u00150\u000eX\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u0017\u0010\u001c\u001a\b\u0012\u0004\u0012\u00020\u00150\u0011\u00a2\u0006\b\n\u0000\u001a\u0004\b\u001c\u0010\u0013R\u0014\u0010\u001d\u001a\b\u0012\u0004\u0012\u00020\u00150\u000eX\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u0017\u0010\u001e\u001a\b\u0012\u0004\u0012\u00020\u00150\u0011\u00a2\u0006\b\n\u0000\u001a\u0004\b\u001f\u0010\u0013R\u0014\u0010 \u001a\b\u0012\u0004\u0012\u00020!0\u000eX\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u0017\u0010\"\u001a\b\u0012\u0004\u0012\u00020!0\u0011\u00a2\u0006\b\n\u0000\u001a\u0004\b#\u0010\u0013R\u0014\u0010$\u001a\b\u0012\u0004\u0012\u00020%0\u000eX\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u0017\u0010&\u001a\b\u0012\u0004\u0012\u00020%0\u0011\u00a2\u0006\b\n\u0000\u001a\u0004\b'\u0010\u0013R\u0014\u0010(\u001a\b\u0012\u0004\u0012\u00020)0\u000eX\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u0017\u0010*\u001a\b\u0012\u0004\u0012\u00020)0\u0011\u00a2\u0006\b\n\u0000\u001a\u0004\b+\u0010\u0013R\u0014\u0010,\u001a\b\u0012\u0004\u0012\u00020-0\u000eX\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u0017\u0010.\u001a\b\u0012\u0004\u0012\u00020-0\u0011\u00a2\u0006\b\n\u0000\u001a\u0004\b/\u0010\u0013R\u0014\u00100\u001a\b\u0012\u0004\u0012\u0002010\u000eX\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u0017\u00102\u001a\b\u0012\u0004\u0012\u0002010\u0011\u00a2\u0006\b\n\u0000\u001a\u0004\b3\u0010\u0013R\u0019\u00104\u001a\n\u0012\u0006\u0012\u0004\u0018\u0001050\u0011\u00a2\u0006\b\n\u0000\u001a\u0004\b6\u0010\u0013R\u001d\u00107\u001a\u000e\u0012\n\u0012\b\u0012\u0004\u0012\u000205080\u0011\u00a2\u0006\b\n\u0000\u001a\u0004\b9\u0010\u0013R\u001d\u0010:\u001a\u000e\u0012\n\u0012\b\u0012\u0004\u0012\u00020;080\u0011\u00a2\u0006\b\n\u0000\u001a\u0004\b<\u0010\u0013R\u0017\u0010=\u001a\b\u0012\u0004\u0012\u00020>0\u0011\u00a2\u0006\b\n\u0000\u001a\u0004\b?\u0010\u0013R\u0016\u0010@\u001a\n\u0012\u0006\u0012\u0004\u0018\u00010A0\u000eX\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u0019\u0010B\u001a\n\u0012\u0006\u0012\u0004\u0018\u00010A0\u0011\u00a2\u0006\b\n\u0000\u001a\u0004\bC\u0010\u0013R\u0014\u0010D\u001a\b\u0012\u0004\u0012\u00020E0\u000eX\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u0017\u0010F\u001a\b\u0012\u0004\u0012\u00020E0\u0011\u00a2\u0006\b\n\u0000\u001a\u0004\bG\u0010\u0013R\u0010\u0010H\u001a\u0004\u0018\u00010IX\u0082\u000e\u00a2\u0006\u0002\n\u0000R\u0014\u0010J\u001a\b\u0012\u0004\u0012\u00020K0\u000eX\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u0017\u0010L\u001a\b\u0012\u0004\u0012\u00020K0\u0011\u00a2\u0006\b\n\u0000\u001a\u0004\bM\u0010\u0013R\u0014\u0010N\u001a\b\u0012\u0004\u0012\u00020\u00150\u000eX\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u0017\u0010O\u001a\b\u0012\u0004\u0012\u00020\u00150\u0011\u00a2\u0006\b\n\u0000\u001a\u0004\bO\u0010\u0013R\u0014\u0010P\u001a\b\u0012\u0004\u0012\u00020\u00150\u000eX\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u0017\u0010Q\u001a\b\u0012\u0004\u0012\u00020\u00150\u0011\u00a2\u0006\b\n\u0000\u001a\u0004\bQ\u0010\u0013R\u0014\u0010R\u001a\b\u0012\u0004\u0012\u00020\u00150\u000eX\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u0017\u0010S\u001a\b\u0012\u0004\u0012\u00020\u00150\u0011\u00a2\u0006\b\n\u0000\u001a\u0004\bS\u0010\u0013R\u0014\u0010T\u001a\b\u0012\u0004\u0012\u00020\u00150\u000eX\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u0017\u0010U\u001a\b\u0012\u0004\u0012\u00020\u00150\u0011\u00a2\u0006\b\n\u0000\u001a\u0004\bU\u0010\u0013R\u0014\u0010V\u001a\b\u0012\u0004\u0012\u00020\u00150\u000eX\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u0017\u0010W\u001a\b\u0012\u0004\u0012\u00020\u00150\u0011\u00a2\u0006\b\n\u0000\u001a\u0004\bW\u0010\u0013R\u0014\u0010X\u001a\b\u0012\u0004\u0012\u00020\u00150\u000eX\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u0017\u0010Y\u001a\b\u0012\u0004\u0012\u00020\u00150\u0011\u00a2\u0006\b\n\u0000\u001a\u0004\bY\u0010\u0013R\u0014\u0010Z\u001a\b\u0012\u0004\u0012\u00020\u00150\u000eX\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u0017\u0010[\u001a\b\u0012\u0004\u0012\u00020\u00150\u0011\u00a2\u0006\b\n\u0000\u001a\u0004\b[\u0010\u0013R\u0016\u0010\\\u001a\n\u0012\u0006\u0012\u0004\u0018\u00010;0\u000eX\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u0019\u0010]\u001a\n\u0012\u0006\u0012\u0004\u0018\u00010;0\u0011\u00a2\u0006\b\n\u0000\u001a\u0004\b^\u0010\u0013R\u0014\u0010_\u001a\b\u0012\u0004\u0012\u00020`0\u000eX\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u0017\u0010a\u001a\b\u0012\u0004\u0012\u00020`0\u0011\u00a2\u0006\b\n\u0000\u001a\u0004\bb\u0010\u0013R\u0014\u0010c\u001a\b\u0012\u0004\u0012\u00020d0\u000eX\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u0017\u0010e\u001a\b\u0012\u0004\u0012\u00020d0\u0011\u00a2\u0006\b\n\u0000\u001a\u0004\bf\u0010\u0013R\u0014\u0010g\u001a\b\u0012\u0004\u0012\u00020h0\u000eX\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u0017\u0010i\u001a\b\u0012\u0004\u0012\u00020h0\u0011\u00a2\u0006\b\n\u0000\u001a\u0004\bj\u0010\u0013R\u0014\u0010k\u001a\b\u0012\u0004\u0012\u00020h0\u000eX\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u0017\u0010l\u001a\b\u0012\u0004\u0012\u00020h0\u0011\u00a2\u0006\b\n\u0000\u001a\u0004\bm\u0010\u0013R\u0014\u0010n\u001a\b\u0012\u0004\u0012\u00020\u00150\u000eX\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u0017\u0010o\u001a\b\u0012\u0004\u0012\u00020\u00150\u0011\u00a2\u0006\b\n\u0000\u001a\u0004\bp\u0010\u0013R\u0014\u0010q\u001a\b\u0012\u0004\u0012\u00020\u00150\u000eX\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u0017\u0010r\u001a\b\u0012\u0004\u0012\u00020\u00150\u0011\u00a2\u0006\b\n\u0000\u001a\u0004\bs\u0010\u0013R\u001a\u0010t\u001a\u000e\u0012\n\u0012\b\u0012\u0004\u0012\u00020`080\u000eX\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u001d\u0010u\u001a\u000e\u0012\n\u0012\b\u0012\u0004\u0012\u00020`080\u0011\u00a2\u0006\b\n\u0000\u001a\u0004\bv\u0010\u0013R\u001a\u0010w\u001a\u000e\u0012\n\u0012\b\u0012\u0004\u0012\u00020`0x0\u000eX\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u001d\u0010y\u001a\u000e\u0012\n\u0012\b\u0012\u0004\u0012\u00020`0x0\u0011\u00a2\u0006\b\n\u0000\u001a\u0004\bz\u0010\u0013R\u0014\u0010{\u001a\b\u0012\u0004\u0012\u00020\u00150\u000eX\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u0017\u0010|\u001a\b\u0012\u0004\u0012\u00020\u00150\u0011\u00a2\u0006\b\n\u0000\u001a\u0004\b|\u0010\u0013R\u0014\u0010}\u001a\b\u0012\u0004\u0012\u00020\u00150\u000eX\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u0017\u0010~\u001a\b\u0012\u0004\u0012\u00020\u00150\u0011\u00a2\u0006\b\n\u0000\u001a\u0004\b\u007f\u0010\u0013R\u0015\u0010\u0080\u0001\u001a\b\u0012\u0004\u0012\u00020E0\u000eX\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u0019\u0010\u0081\u0001\u001a\b\u0012\u0004\u0012\u00020E0\u0011\u00a2\u0006\t\n\u0000\u001a\u0005\b\u0082\u0001\u0010\u0013R\u0012\u0010\u0083\u0001\u001a\u0005\u0018\u00010\u0084\u0001X\u0082\u000e\u00a2\u0006\u0002\n\u0000R\u0015\u0010\u0085\u0001\u001a\b\u0012\u0004\u0012\u00020d0\u000eX\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u0019\u0010\u0086\u0001\u001a\b\u0012\u0004\u0012\u00020d0\u0011\u00a2\u0006\t\n\u0000\u001a\u0005\b\u0087\u0001\u0010\u0013R\u0012\u0010\u0088\u0001\u001a\u0005\u0018\u00010\u0084\u0001X\u0082\u000e\u00a2\u0006\u0002\n\u0000R\u001a\u0010\u0089\u0001\u001a\t\u0012\u0005\u0012\u00030\u008a\u00010\u0011\u00a2\u0006\t\n\u0000\u001a\u0005\b\u008b\u0001\u0010\u0013R\u0016\u0010\u00ac\u0001\u001a\t\u0012\u0004\u0012\u00020K0\u00ad\u0001X\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u0014\u0010\u00b2\u0001\u001a\u00020\u00158F\u00a2\u0006\b\u001a\u0006\b\u00b3\u0001\u0010\u00b4\u0001\u00a8\u0006\u008a\u0002"}, d2={"Lcom/example/ui/viewmodel/ParkingViewModel;", "Landroidx/lifecycle/AndroidViewModel;", "application", "Landroid/app/Application;", "<init>", "(Landroid/app/Application;)V", "repository", "Lcom/example/data/repository/ParkingRepository;", "compassSensorManager", "Lcom/example/sensor/CompassSensorManager;", "prefs", "Landroid/content/SharedPreferences;", "kotlin.jvm.PlatformType", "_themeMode", "Lkotlinx/coroutines/flow/MutableStateFlow;", "Lcom/example/ui/theme/AppThemeMode;", "themeMode", "Lkotlinx/coroutines/flow/StateFlow;", "getThemeMode", "()Lkotlinx/coroutines/flow/StateFlow;", "_oledMode", "", "oledMode", "getOledMode", "_autoSunTheme", "autoSunTheme", "getAutoSunTheme", "_isDaytime", "isDaytime", "_dynamicColor", "dynamicColor", "getDynamicColor", "_carBadgeStyle", "Lcom/example/ui/components/CarBadgeStyle;", "carBadgeStyle", "getCarBadgeStyle", "_appLanguage", "Lcom/example/ui/i18n/AppLanguage;", "appLanguage", "getAppLanguage", "_hapticProfile", "Lcom/example/util/HapticProfile;", "hapticProfile", "getHapticProfile", "_hardwareProfile", "Lcom/example/sensor/DeviceHardwareProfile;", "hardwareProfile", "getHardwareProfile", "_waypointHapticMode", "Lcom/example/sensor/WaypointHapticMode;", "waypointHapticMode", "getWaypointHapticMode", "activeSpot", "Lcom/example/data/entity/ParkingSpot;", "getActiveSpot", "allSpots", "", "getAllSpots", "allDevices", "Lcom/example/data/entity/BluetoothCarDevice;", "getAllDevices", "compassState", "Lcom/example/sensor/CompassState;", "getCompassState", "_currentLocation", "Landroid/location/Location;", "currentLocation", "getCurrentLocation", "_currentAddress", "", "currentAddress", "getCurrentAddress", "locationCallback", "Lcom/google/android/gms/location/LocationCallback;", "_selectedTab", "Lcom/example/ui/viewmodel/AppTab;", "selectedTab", "getSelectedTab", "_isGpsRefreshing", "isGpsRefreshing", "_isRadarServiceRunning", "isRadarServiceRunning", "_isDeveloperUnlocked", "isDeveloperUnlocked", "_isDevMockGpsEnabled", "isDevMockGpsEnabled", "_isBtProximityEnabled", "isBtProximityEnabled", "_isDevHapticDiagnostics", "isDevHapticDiagnostics", "_isParkingTimerFeatureEnabled", "isParkingTimerFeatureEnabled", "_activeBtProximityDevice", "activeBtProximityDevice", "getActiveBtProximityDevice", "_btProximityRssi", "", "btProximityRssi", "getBtProximityRssi", "_btProximityDistanceMeters", "", "btProximityDistanceMeters", "getBtProximityDistanceMeters", "_timerTotalSeconds", "", "timerTotalSeconds", "getTimerTotalSeconds", "_timerRemainingSeconds", "timerRemainingSeconds", "getTimerRemainingSeconds", "_timerIsRunning", "timerIsRunning", "getTimerIsRunning", "_timerIsPaused", "timerIsPaused", "getTimerIsPaused", "_timerRemindersMinutes", "timerRemindersMinutes", "getTimerRemindersMinutes", "_timerAlertsTriggered", "", "timerAlertsTriggered", "getTimerAlertsTriggered", "_isAlarmActive", "isAlarmActive", "_showTimerDialog", "showTimerDialog", "getShowTimerDialog", "_alarmSoundTitle", "alarmSoundTitle", "getAlarmSoundTitle", "timerJob", "Lkotlinx/coroutines/Job;", "_btProximityRelativeAngle", "btProximityRelativeAngle", "getBtProximityRelativeAngle", "btProximityJob", "navigationTelemetry", "Lcom/example/ui/viewmodel/NavigationTelemetry;", "getNavigationTelemetry", "startLocationTracking", "", "stopLocationTracking", "searchAddressCoordinates", "Lkotlin/Pair;", "", "query", "(Ljava/lang/String;Lkotlin/coroutines/Continuation;)Ljava/lang/Object;", "getAddressForCoordinates", "lat", "lng", "(DDLkotlin/coroutines/Continuation;)Ljava/lang/Object;", "setThemeMode", "mode", "setAutoSunTheme", "enabled", "updateDaytimeState", "setOledMode", "setDynamicColor", "setCarBadgeStyle", "style", "setAppLanguage", "language", "setHapticProfile", "profile", "setWaypointHapticMode", "triggerHapticTest", "event", "Lcom/example/sensor/HapticPatternEvent;", "startWaypointHapticService", "spotName", "stopWaypointHapticService", "tabBackStack", "", "selectTab", "tab", "addToHistory", "navigateBack", "canNavigateBack", "getCanNavigateBack", "()Z", "startCompass", "stopCompass", "recalibrateCompass", "dismissCompassCalibration", "refreshCurrentLocation", "onComplete", "Lkotlin/Function1;", "saveCurrentLocationAsParking", "floorLevel", "note", "customLat", "customLng", "customAddress", "(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/lang/Double;Ljava/lang/Double;Ljava/lang/String;)V", "updateSpotDetails", "id", "newName", "newFloor", "newNote", "meterMinutes", "(JLjava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/lang/Integer;)V", "updateSpotNote", "spotId", "updateHistorySpot", "name", "floor", "(JLjava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/lang/Double;Ljava/lang/Double;Ljava/lang/String;)V", "setActiveNavigationTarget", "spot", "setParkingMeter", "minutes", "markCarFound", "deleteSpot", "clearHistory", "syncSystemPairedDevices", "selectPrimaryCarDevice", "address", "toggleMonitoredDevice", "isMonitored", "renameBluetoothDevice", "deleteBluetoothDevice", "registerNewDevice", "deviceType", "openSystemBluetoothSettings", "context", "Landroid/content/Context;", "simulateBluetoothDisconnect", "deviceName", "deviceAddress", "openGoogleMapsNavigation", "shareParkingLocation", "toggleRadarService", "stopRadarService", "unlockDeveloperMode", "setDevMockGpsEnabled", "setBtProximityEnabled", "setDevHapticDiagnostics", "setParkingTimerFeatureEnabled", "startBtProximityFinder", "device", "stopBtProximityFinder", "openParkingTimer", "closeParkingTimer", "startParkingTimer", "pauseParkingTimer", "resetParkingTimer", "resetTimerToZero", "toggleTimerPlayPause", "addTimerMinutes", "setTimerDuration", "seconds", "adjustTimerByDrag", "isClockwise", "adjustReminderMinutes", "index", "isAdd", "setTimerReminders", "reminders", "dismissTimerAlarm", "snoozeTimerAlarm", "updateAlarmSoundUri", "uri", "Landroid/net/Uri;", "refreshAlarmTitle", "onCleared", "app"})
@StabilityInferred(parameters=0)
@SourceDebugExtension(value={"SMAP\nParkingViewModel.kt\nKotlin\n*S Kotlin\n*F\n+ 1 ParkingViewModel.kt\ncom/example/ui/viewmodel/ParkingViewModel\n+ 2 _Collections.kt\nkotlin/collections/CollectionsKt___CollectionsKt\n+ 3 fake.kt\nkotlin/jvm/internal/FakeKt\n*L\n1#1,1136:1\n1617#2,9:1137\n1869#2:1146\n1870#2:1148\n1626#2:1149\n774#2:1150\n865#2,2:1151\n774#2:1154\n865#2,2:1155\n1#3:1147\n1#3:1153\n*S KotlinDebug\n*F\n+ 1 ParkingViewModel.kt\ncom/example/ui/viewmodel/ParkingViewModel\n*L\n202#1:1137,9\n202#1:1146\n202#1:1148\n202#1:1149\n203#1:1150\n203#1:1151,2\n1096#1:1154\n1096#1:1155,2\n202#1:1147\n*E\n"})
public final class ParkingViewModel
extends AndroidViewModel {
    @NotNull
    private final ParkingRepository repository;
    @NotNull
    private final CompassSensorManager compassSensorManager;
    private final SharedPreferences prefs;
    @NotNull
    private final MutableStateFlow<AppThemeMode> _themeMode;
    @NotNull
    private final StateFlow<AppThemeMode> themeMode;
    @NotNull
    private final MutableStateFlow<Boolean> _oledMode;
    @NotNull
    private final StateFlow<Boolean> oledMode;
    @NotNull
    private final MutableStateFlow<Boolean> _autoSunTheme;
    @NotNull
    private final StateFlow<Boolean> autoSunTheme;
    @NotNull
    private final MutableStateFlow<Boolean> _isDaytime;
    @NotNull
    private final StateFlow<Boolean> isDaytime;
    @NotNull
    private final MutableStateFlow<Boolean> _dynamicColor;
    @NotNull
    private final StateFlow<Boolean> dynamicColor;
    @NotNull
    private final MutableStateFlow<CarBadgeStyle> _carBadgeStyle;
    @NotNull
    private final StateFlow<CarBadgeStyle> carBadgeStyle;
    @NotNull
    private final MutableStateFlow<AppLanguage> _appLanguage;
    @NotNull
    private final StateFlow<AppLanguage> appLanguage;
    @NotNull
    private final MutableStateFlow<HapticProfile> _hapticProfile;
    @NotNull
    private final StateFlow<HapticProfile> hapticProfile;
    @NotNull
    private final MutableStateFlow<DeviceHardwareProfile> _hardwareProfile;
    @NotNull
    private final StateFlow<DeviceHardwareProfile> hardwareProfile;
    @NotNull
    private final MutableStateFlow<WaypointHapticMode> _waypointHapticMode;
    @NotNull
    private final StateFlow<WaypointHapticMode> waypointHapticMode;
    @NotNull
    private final StateFlow<ParkingSpot> activeSpot;
    @NotNull
    private final StateFlow<List<ParkingSpot>> allSpots;
    @NotNull
    private final StateFlow<List<BluetoothCarDevice>> allDevices;
    @NotNull
    private final StateFlow<CompassState> compassState;
    @NotNull
    private final MutableStateFlow<Location> _currentLocation;
    @NotNull
    private final StateFlow<Location> currentLocation;
    @NotNull
    private final MutableStateFlow<String> _currentAddress;
    @NotNull
    private final StateFlow<String> currentAddress;
    @Nullable
    private LocationCallback locationCallback;
    @NotNull
    private final MutableStateFlow<AppTab> _selectedTab;
    @NotNull
    private final StateFlow<AppTab> selectedTab;
    @NotNull
    private final MutableStateFlow<Boolean> _isGpsRefreshing;
    @NotNull
    private final StateFlow<Boolean> isGpsRefreshing;
    @NotNull
    private final MutableStateFlow<Boolean> _isRadarServiceRunning;
    @NotNull
    private final StateFlow<Boolean> isRadarServiceRunning;
    @NotNull
    private final MutableStateFlow<Boolean> _isDeveloperUnlocked;
    @NotNull
    private final StateFlow<Boolean> isDeveloperUnlocked;
    @NotNull
    private final MutableStateFlow<Boolean> _isDevMockGpsEnabled;
    @NotNull
    private final StateFlow<Boolean> isDevMockGpsEnabled;
    @NotNull
    private final MutableStateFlow<Boolean> _isBtProximityEnabled;
    @NotNull
    private final StateFlow<Boolean> isBtProximityEnabled;
    @NotNull
    private final MutableStateFlow<Boolean> _isDevHapticDiagnostics;
    @NotNull
    private final StateFlow<Boolean> isDevHapticDiagnostics;
    @NotNull
    private final MutableStateFlow<Boolean> _isParkingTimerFeatureEnabled;
    @NotNull
    private final StateFlow<Boolean> isParkingTimerFeatureEnabled;
    @NotNull
    private final MutableStateFlow<BluetoothCarDevice> _activeBtProximityDevice;
    @NotNull
    private final StateFlow<BluetoothCarDevice> activeBtProximityDevice;
    @NotNull
    private final MutableStateFlow<Integer> _btProximityRssi;
    @NotNull
    private final StateFlow<Integer> btProximityRssi;
    @NotNull
    private final MutableStateFlow<Float> _btProximityDistanceMeters;
    @NotNull
    private final StateFlow<Float> btProximityDistanceMeters;
    @NotNull
    private final MutableStateFlow<Long> _timerTotalSeconds;
    @NotNull
    private final StateFlow<Long> timerTotalSeconds;
    @NotNull
    private final MutableStateFlow<Long> _timerRemainingSeconds;
    @NotNull
    private final StateFlow<Long> timerRemainingSeconds;
    @NotNull
    private final MutableStateFlow<Boolean> _timerIsRunning;
    @NotNull
    private final StateFlow<Boolean> timerIsRunning;
    @NotNull
    private final MutableStateFlow<Boolean> _timerIsPaused;
    @NotNull
    private final StateFlow<Boolean> timerIsPaused;
    @NotNull
    private final MutableStateFlow<List<Integer>> _timerRemindersMinutes;
    @NotNull
    private final StateFlow<List<Integer>> timerRemindersMinutes;
    @NotNull
    private final MutableStateFlow<Set<Integer>> _timerAlertsTriggered;
    @NotNull
    private final StateFlow<Set<Integer>> timerAlertsTriggered;
    @NotNull
    private final MutableStateFlow<Boolean> _isAlarmActive;
    @NotNull
    private final StateFlow<Boolean> isAlarmActive;
    @NotNull
    private final MutableStateFlow<Boolean> _showTimerDialog;
    @NotNull
    private final StateFlow<Boolean> showTimerDialog;
    @NotNull
    private final MutableStateFlow<String> _alarmSoundTitle;
    @NotNull
    private final StateFlow<String> alarmSoundTitle;
    @Nullable
    private Job timerJob;
    @NotNull
    private final MutableStateFlow<Float> _btProximityRelativeAngle;
    @NotNull
    private final StateFlow<Float> btProximityRelativeAngle;
    @Nullable
    private Job btProximityJob;
    @NotNull
    private final StateFlow<NavigationTelemetry> navigationTelemetry;
    @NotNull
    private final List<AppTab> tabBackStack;
    public static final int $stable = 8;

    /*
     * Unable to fully structure code
     */
    public ParkingViewModel(@NotNull Application application) {
        Intrinsics.checkNotNullParameter((Object)application, (String)"application");
        super(application);
        this.repository = ((AutoParkApplication)application).getRepository();
        this.compassSensorManager = new CompassSensorManager((Context)application);
        this.prefs = application.getSharedPreferences("pixel_parking_prefs", 0);
        var20_2 = this;
        try {
            v0 = var20_2;
            v1 = this.prefs.getString("theme_mode", "SYSTEM");
            if (v1 == null) {
                v1 = "SYSTEM";
            }
            var2_3 = AppThemeMode.valueOf(v1);
        }
        catch (Exception var3_4) {
            v0 = var20_2;
            var2_3 = AppThemeMode.SYSTEM;
        }
        v0._themeMode = StateFlowKt.MutableStateFlow((Object)var2_3);
        this.themeMode = FlowKt.asStateFlow(this._themeMode);
        this._oledMode = StateFlowKt.MutableStateFlow((Object)this.prefs.getBoolean("oled_mode", false));
        this.oledMode = FlowKt.asStateFlow(this._oledMode);
        this._autoSunTheme = StateFlowKt.MutableStateFlow((Object)this.prefs.getBoolean("auto_sun_theme", false));
        this.autoSunTheme = FlowKt.asStateFlow(this._autoSunTheme);
        this._isDaytime = StateFlowKt.MutableStateFlow((Object)true);
        this.isDaytime = FlowKt.asStateFlow(this._isDaytime);
        this._dynamicColor = StateFlowKt.MutableStateFlow((Object)this.prefs.getBoolean("dynamic_color", true));
        this.dynamicColor = FlowKt.asStateFlow(this._dynamicColor);
        var20_2 = this;
        try {
            v2 = var20_2;
            v3 = this.prefs.getString("car_badge_style", "CINEMATIC");
            if (v3 == null) {
                v3 = "CINEMATIC";
            }
            var2_3 = CarBadgeStyle.valueOf(v3);
        }
        catch (Exception <unused var>) {
            v2 = var20_2;
            var2_3 = CarBadgeStyle.CINEMATIC;
        }
        v2._carBadgeStyle = StateFlowKt.MutableStateFlow((Object)var2_3);
        this.carBadgeStyle = FlowKt.asStateFlow(this._carBadgeStyle);
        var20_2 = this;
        try {
            v4 = var20_2;
            v5 = this.prefs.getString("app_language", "ENGLISH");
            if (v5 == null) {
                v5 = "ENGLISH";
            }
            var2_3 = AppLanguage.valueOf(v5);
        }
        catch (Exception <unused var>) {
            v4 = var20_2;
            var2_3 = AppLanguage.ENGLISH;
        }
        v4._appLanguage = StateFlowKt.MutableStateFlow((Object)var2_3);
        this.appLanguage = FlowKt.asStateFlow(this._appLanguage);
        var20_2 = this;
        try {
            v6 = var20_2;
            v7 = this.prefs.getString("haptic_profile", "AUTO");
            if (v7 == null) {
                v7 = "AUTO";
            }
            var2_3 = HapticProfile.valueOf(v7);
        }
        catch (Exception <unused var>) {
            v6 = var20_2;
            var2_3 = HapticProfile.AUTO;
        }
        v6._hapticProfile = StateFlowKt.MutableStateFlow((Object)var2_3);
        this.hapticProfile = FlowKt.asStateFlow(this._hapticProfile);
        this._hardwareProfile = StateFlowKt.MutableStateFlow((Object)HardwareSensorProfiler.INSTANCE.profileDevice((Context)application));
        this.hardwareProfile = FlowKt.asStateFlow(this._hardwareProfile);
        var20_2 = this;
        try {
            v8 = var20_2;
            v9 = this.prefs.getString("waypoint_haptic_mode", "AUTO_ADAPTIVE");
            if (v9 == null) {
                v9 = "AUTO_ADAPTIVE";
            }
            var2_3 = WaypointHapticMode.valueOf(v9);
        }
        catch (Exception <unused var>) {
            v8 = var20_2;
            var2_3 = WaypointHapticMode.AUTO_ADAPTIVE;
        }
        v8._waypointHapticMode = StateFlowKt.MutableStateFlow((Object)var2_3);
        this.waypointHapticMode = FlowKt.asStateFlow(this._waypointHapticMode);
        this.activeSpot = FlowKt.stateIn(this.repository.getActiveSpot(), (CoroutineScope)ViewModelKt.getViewModelScope((ViewModel)((ViewModel)this)), (SharingStarted)SharingStarted.Companion.WhileSubscribed$default((SharingStarted.Companion)SharingStarted.Companion, (long)5000L, (long)0L, (int)2, null), null);
        this.allSpots = FlowKt.stateIn(this.repository.getAllSpots(), (CoroutineScope)ViewModelKt.getViewModelScope((ViewModel)((ViewModel)this)), (SharingStarted)SharingStarted.Companion.WhileSubscribed$default((SharingStarted.Companion)SharingStarted.Companion, (long)5000L, (long)0L, (int)2, null), (Object)CollectionsKt.emptyList());
        this.allDevices = FlowKt.stateIn(this.repository.getAllDevices(), (CoroutineScope)ViewModelKt.getViewModelScope((ViewModel)((ViewModel)this)), (SharingStarted)SharingStarted.Companion.WhileSubscribed$default((SharingStarted.Companion)SharingStarted.Companion, (long)5000L, (long)0L, (int)2, null), (Object)CollectionsKt.emptyList());
        this.compassState = this.compassSensorManager.getCompassState();
        this._currentLocation = StateFlowKt.MutableStateFlow(null);
        this.currentLocation = FlowKt.asStateFlow(this._currentLocation);
        this._currentAddress = StateFlowKt.MutableStateFlow((Object)"");
        this.currentAddress = FlowKt.asStateFlow(this._currentAddress);
        this._selectedTab = StateFlowKt.MutableStateFlow((Object)AppTab.DASHBOARD);
        this.selectedTab = FlowKt.asStateFlow(this._selectedTab);
        this._isGpsRefreshing = StateFlowKt.MutableStateFlow((Object)false);
        this.isGpsRefreshing = FlowKt.asStateFlow(this._isGpsRefreshing);
        this._isRadarServiceRunning = StateFlowKt.MutableStateFlow((Object)false);
        this.isRadarServiceRunning = FlowKt.asStateFlow(this._isRadarServiceRunning);
        this._isDeveloperUnlocked = StateFlowKt.MutableStateFlow((Object)this.prefs.getBoolean("dev_mode_unlocked", false));
        this.isDeveloperUnlocked = FlowKt.asStateFlow(this._isDeveloperUnlocked);
        this._isDevMockGpsEnabled = StateFlowKt.MutableStateFlow((Object)this.prefs.getBoolean("dev_mock_gps", false));
        this.isDevMockGpsEnabled = FlowKt.asStateFlow(this._isDevMockGpsEnabled);
        this._isBtProximityEnabled = StateFlowKt.MutableStateFlow((Object)this.prefs.getBoolean("bt_proximity_enabled", false));
        this.isBtProximityEnabled = FlowKt.asStateFlow(this._isBtProximityEnabled);
        this._isDevHapticDiagnostics = StateFlowKt.MutableStateFlow((Object)this.prefs.getBoolean("dev_haptic_diag", false));
        this.isDevHapticDiagnostics = FlowKt.asStateFlow(this._isDevHapticDiagnostics);
        this._isParkingTimerFeatureEnabled = StateFlowKt.MutableStateFlow((Object)this.prefs.getBoolean("parking_timer_feature_enabled", false));
        this.isParkingTimerFeatureEnabled = FlowKt.asStateFlow(this._isParkingTimerFeatureEnabled);
        this._activeBtProximityDevice = StateFlowKt.MutableStateFlow(null);
        this.activeBtProximityDevice = FlowKt.asStateFlow(this._activeBtProximityDevice);
        this._btProximityRssi = StateFlowKt.MutableStateFlow((Object)-60);
        this.btProximityRssi = FlowKt.asStateFlow(this._btProximityRssi);
        this._btProximityDistanceMeters = StateFlowKt.MutableStateFlow((Object)Float.valueOf(8.0f));
        this.btProximityDistanceMeters = FlowKt.asStateFlow(this._btProximityDistanceMeters);
        this._timerTotalSeconds = StateFlowKt.MutableStateFlow((Object)RangesKt.coerceIn((long)this.prefs.getLong("parking_timer_total_seconds", 1800L), (long)60L, (long)86400L));
        this.timerTotalSeconds = FlowKt.asStateFlow(this._timerTotalSeconds);
        this._timerRemainingSeconds = StateFlowKt.MutableStateFlow((Object)RangesKt.coerceIn((long)this.prefs.getLong("parking_timer_total_seconds", 1800L), (long)60L, (long)86400L));
        this.timerRemainingSeconds = FlowKt.asStateFlow(this._timerRemainingSeconds);
        this._timerIsRunning = StateFlowKt.MutableStateFlow((Object)false);
        this.timerIsRunning = FlowKt.asStateFlow(this._timerIsRunning);
        this._timerIsPaused = StateFlowKt.MutableStateFlow((Object)false);
        this.timerIsPaused = FlowKt.asStateFlow(this._timerIsPaused);
        v10 = this;
        var2_3 = this.prefs.getString("parking_timer_reminders", "15,10,5");
        if (var2_3 == null || (var3_6 = StringsKt.split$default((CharSequence)((CharSequence)var2_3), (String[])(var4_10 = new String[]{","}), (boolean)false, (int)0, (int)6, null)) == null) ** GOTO lbl-1000
        var5_11 = var3_6;
        var20_2 = v10;
        $i$f$mapNotNull\1\202 = false;
        var7_14 = $this$mapNotNull\1;
        destination\2 = new ArrayList<E>();
        $i$f$mapNotNullTo\2\1137 = false;
        $this$forEach\3 = $this$mapNotNullTo\2;
        $i$f$forEach\3\1145 = false;
        var12_26 = $this$forEach\3.iterator();
        while (var12_26.hasNext()) {
            element\4 = element\3 = var12_26.next();
            $i$a$-forEach-CollectionsKt___CollectionsKt$mapNotNullTo$1\4\1146\2 = false;
            it\6 = (String)element\4;
            $i$a$-mapNotNull-ParkingViewModel$_timerRemindersMinutes$1\6\1145\0 = false;
            if (StringsKt.toIntOrNull((String)StringsKt.trim((CharSequence)it\6).toString()) == null) continue;
            $i$a$-let-CollectionsKt___CollectionsKt$mapNotNullTo$1$1\5\1147\4 = false;
            destination\2.add(it\4);
        }
        $i$f$mapNotNull\1\202 = (List)destination\2;
        $i$f$filter\7\203 = false;
        destination\2 = $this$filter\7;
        destination\8 = new ArrayList<E>();
        $i$f$filterTo\8\1150 = false;
        for (T element\8 : $this$filterTo\8) {
            it\9 = ((Number)element\8).intValue();
            $i$a$-filter-ParkingViewModel$_timerRemindersMinutes$2\9\1151\0 = false;
            if (!(it\9 > 0)) continue;
            destination\8.add(element\8);
        }
        v10 = var20_2;
        var6_13 = CollectionsKt.take((Iterable)((List)destination\8), (int)3);
        if (var6_13 == null) ** GOTO lbl-1000
        var8_17 = var6_13;
        if (var8_17.isEmpty()) {
            var20_2 = v10;
            $i$a$-ifEmpty-ParkingViewModel$_timerRemindersMinutes$3\10\205\0 = false;
            var10_23 = new Integer[]{15, 10, 5};
            v11 = CollectionsKt.listOf((Object[])var10_23);
            v10 = var20_2;
        } else {
            v11 = var8_17;
        }
        var7_16 = (List)v11;
        if (var7_16 != null) {
            v12 = var7_16;
        } else lbl-1000:
        // 3 sources

        {
            var8_17 = new Integer[]{15, 10, 5};
            v12 = CollectionsKt.listOf((Object[])var8_17);
        }
        v10._timerRemindersMinutes = StateFlowKt.MutableStateFlow((Object)v12);
        this.timerRemindersMinutes = FlowKt.asStateFlow(this._timerRemindersMinutes);
        this._timerAlertsTriggered = StateFlowKt.MutableStateFlow((Object)SetsKt.emptySet());
        this.timerAlertsTriggered = FlowKt.asStateFlow(this._timerAlertsTriggered);
        this._isAlarmActive = StateFlowKt.MutableStateFlow((Object)false);
        this.isAlarmActive = FlowKt.asStateFlow(this._isAlarmActive);
        this._showTimerDialog = StateFlowKt.MutableStateFlow((Object)false);
        this.showTimerDialog = FlowKt.asStateFlow(this._showTimerDialog);
        this._alarmSoundTitle = StateFlowKt.MutableStateFlow((Object)AlarmSoundHelper.getAlarmTitle$default(AlarmSoundHelper.INSTANCE, (Context)application, null, 2, null));
        this.alarmSoundTitle = FlowKt.asStateFlow(this._alarmSoundTitle);
        this._btProximityRelativeAngle = StateFlowKt.MutableStateFlow((Object)Float.valueOf(0.0f));
        this.btProximityRelativeAngle = FlowKt.asStateFlow(this._btProximityRelativeAngle);
        this.navigationTelemetry = FlowKt.stateIn((Flow)FlowKt.combine((Flow)((Flow)this.activeSpot), (Flow)((Flow)this.currentLocation), (Flow)((Flow)this.compassState), (Function4)((Function4)new Function4<ParkingSpot, Location, CompassState, Continuation<? super NavigationTelemetry>, Object>(this, null){
            int label;
            /* synthetic */ Object L$0;
            /* synthetic */ Object L$1;
            /* synthetic */ Object L$2;
            final /* synthetic */ ParkingViewModel this$0;
            {
                this.this$0 = $receiver;
                super(4, $completion);
            }

            /*
             * WARNING - void declaration
             */
            public final Object invokeSuspend(Object $result) {
                ParkingSpot parkingSpot = (ParkingSpot)this.L$0;
                Location location = (Location)this.L$1;
                CompassState compassState = (CompassState)this.L$2;
                IntrinsicsKt.getCOROUTINE_SUSPENDED();
                switch (this.label) {
                    case 0: {
                        NavigationTelemetry navigationTelemetry2;
                        void loc;
                        void spot;
                        ResultKt.throwOnFailure((Object)$result);
                        if (spot == null || loc == null || !spot.isActive()) {
                            ParkingViewModel.access$getCompassSensorManager$p(this.this$0).setTargetBearing(null);
                            navigationTelemetry2 = new NavigationTelemetry(0.0f, "No GPS Fix", 0.0f, 0.0f, 0.0, false);
                        } else {
                            void it\1;
                            void compass;
                            float dist = LocationHelper.INSTANCE.calculateDistanceMeters(loc.getLatitude(), loc.getLongitude(), spot.getLatitude(), spot.getLongitude());
                            float bearing = LocationHelper.INSTANCE.calculateBearingDegrees(loc.getLatitude(), loc.getLongitude(), spot.getLatitude(), spot.getLongitude());
                            float relativeAngle = (bearing - compass.getAzimuthDegrees() + 360.0f) % 360.0f;
                            Double d = Boxing.boxDouble((double)loc.getAltitude());
                            double d2 = ((Number)d).doubleValue();
                            double d3 = spot.getAltitude();
                            boolean bl = false;
                            boolean bl2 = !(it\1 == 0.0);
                            Double d4 = bl2 ? d : null;
                            double altDiff = d3 - (d4 != null ? d4.doubleValue() : spot.getAltitude());
                            if (dist <= 3.0f) {
                                ParkingViewModel.access$getCompassSensorManager$p(this.this$0).setTargetBearing(null);
                            } else {
                                ParkingViewModel.access$getCompassSensorManager$p(this.this$0).setTargetBearing(Boxing.boxFloat((float)bearing));
                            }
                            navigationTelemetry2 = new NavigationTelemetry(dist, LocationHelper.INSTANCE.formatDistance(dist), bearing, relativeAngle, altDiff, true);
                        }
                        return navigationTelemetry2;
                    }
                }
                throw new IllegalStateException("call to 'resume' before 'invoke' with coroutine");
            }

            public final Object invoke(ParkingSpot p1, Location p2, CompassState p3, Continuation<? super NavigationTelemetry> p4) {
                var var5_5 = new /* invalid duplicate definition of identical inner class */;
                var5_5.L$0 = p1;
                var5_5.L$1 = p2;
                var5_5.L$2 = p3;
                return var5_5.invokeSuspend(Unit.INSTANCE);
            }
        })), (CoroutineScope)ViewModelKt.getViewModelScope((ViewModel)((ViewModel)this)), (SharingStarted)SharingStarted.Companion.WhileSubscribed$default((SharingStarted.Companion)SharingStarted.Companion, (long)5000L, (long)0L, (int)2, null), (Object)new NavigationTelemetry(0.0f, "--", 0.0f, 0.0f, 0.0, false));
        this.compassSensorManager.setHapticProfile((HapticProfile)this._hapticProfile.getValue());
        this.startLocationTracking();
        ParkingViewModel.refreshCurrentLocation$default(this, null, 1, null);
        this.syncSystemPairedDevices();
        this.updateDaytimeState();
        BuildersKt.launch$default((CoroutineScope)ViewModelKt.getViewModelScope((ViewModel)((ViewModel)this)), null, null, (Function2)((Function2)new Function2<CoroutineScope, Continuation<? super Unit>, Object>(this, null){
            int label;
            private /* synthetic */ Object L$0;
            final /* synthetic */ ParkingViewModel this$0;
            {
                this.this$0 = $receiver;
                super(2, $completion);
            }

            /*
             * Unable to fully structure code
             */
            public final Object invokeSuspend(Object $result) {
                var2_2 = (CoroutineScope)this.L$0;
                var3_3 = IntrinsicsKt.getCOROUTINE_SUSPENDED();
                switch (this.label) {
                    case 0: {
                        ResultKt.throwOnFailure((Object)$result);
lbl6:
                        // 3 sources

                        while (CoroutineScopeKt.isActive((CoroutineScope)$this$launch)) {
                            this.this$0.updateDaytimeState();
                            this.L$0 = $this$launch;
                            this.label = 1;
                            v0 = DelayKt.delay((long)60000L, (Continuation)((Continuation)this));
                            if (v0 != var3_3) continue;
                            return var3_3;
                        }
                        break;
                    }
                    case 1: {
                        ResultKt.throwOnFailure((Object)$result);
                        v0 = $result;
                        ** GOTO lbl6
                    }
                }
                return Unit.INSTANCE;
                throw new IllegalStateException("call to 'resume' before 'invoke' with coroutine");
            }

            public final Continuation<Unit> create(Object value, Continuation<?> $completion) {
                var var3_3 = new /* invalid duplicate definition of identical inner class */;
                var3_3.L$0 = value;
                return (Continuation)var3_3;
            }

            public final Object invoke(CoroutineScope p1, Continuation<? super Unit> p2) {
                return (this.create(p1, p2)).invokeSuspend(Unit.INSTANCE);
            }
        }), (int)3, null);
        this.tabBackStack = new ArrayList<E>();
    }

    @NotNull
    public final StateFlow<AppThemeMode> getThemeMode() {
        return this.themeMode;
    }

    @NotNull
    public final StateFlow<Boolean> getOledMode() {
        return this.oledMode;
    }

    @NotNull
    public final StateFlow<Boolean> getAutoSunTheme() {
        return this.autoSunTheme;
    }

    @NotNull
    public final StateFlow<Boolean> isDaytime() {
        return this.isDaytime;
    }

    @NotNull
    public final StateFlow<Boolean> getDynamicColor() {
        return this.dynamicColor;
    }

    @NotNull
    public final StateFlow<CarBadgeStyle> getCarBadgeStyle() {
        return this.carBadgeStyle;
    }

    @NotNull
    public final StateFlow<AppLanguage> getAppLanguage() {
        return this.appLanguage;
    }

    @NotNull
    public final StateFlow<HapticProfile> getHapticProfile() {
        return this.hapticProfile;
    }

    @NotNull
    public final StateFlow<DeviceHardwareProfile> getHardwareProfile() {
        return this.hardwareProfile;
    }

    @NotNull
    public final StateFlow<WaypointHapticMode> getWaypointHapticMode() {
        return this.waypointHapticMode;
    }

    @NotNull
    public final StateFlow<ParkingSpot> getActiveSpot() {
        return this.activeSpot;
    }

    @NotNull
    public final StateFlow<List<ParkingSpot>> getAllSpots() {
        return this.allSpots;
    }

    @NotNull
    public final StateFlow<List<BluetoothCarDevice>> getAllDevices() {
        return this.allDevices;
    }

    @NotNull
    public final StateFlow<CompassState> getCompassState() {
        return this.compassState;
    }

    @NotNull
    public final StateFlow<Location> getCurrentLocation() {
        return this.currentLocation;
    }

    @NotNull
    public final StateFlow<String> getCurrentAddress() {
        return this.currentAddress;
    }

    @NotNull
    public final StateFlow<AppTab> getSelectedTab() {
        return this.selectedTab;
    }

    @NotNull
    public final StateFlow<Boolean> isGpsRefreshing() {
        return this.isGpsRefreshing;
    }

    @NotNull
    public final StateFlow<Boolean> isRadarServiceRunning() {
        return this.isRadarServiceRunning;
    }

    @NotNull
    public final StateFlow<Boolean> isDeveloperUnlocked() {
        return this.isDeveloperUnlocked;
    }

    @NotNull
    public final StateFlow<Boolean> isDevMockGpsEnabled() {
        return this.isDevMockGpsEnabled;
    }

    @NotNull
    public final StateFlow<Boolean> isBtProximityEnabled() {
        return this.isBtProximityEnabled;
    }

    @NotNull
    public final StateFlow<Boolean> isDevHapticDiagnostics() {
        return this.isDevHapticDiagnostics;
    }

    @NotNull
    public final StateFlow<Boolean> isParkingTimerFeatureEnabled() {
        return this.isParkingTimerFeatureEnabled;
    }

    @NotNull
    public final StateFlow<BluetoothCarDevice> getActiveBtProximityDevice() {
        return this.activeBtProximityDevice;
    }

    @NotNull
    public final StateFlow<Integer> getBtProximityRssi() {
        return this.btProximityRssi;
    }

    @NotNull
    public final StateFlow<Float> getBtProximityDistanceMeters() {
        return this.btProximityDistanceMeters;
    }

    @NotNull
    public final StateFlow<Long> getTimerTotalSeconds() {
        return this.timerTotalSeconds;
    }

    @NotNull
    public final StateFlow<Long> getTimerRemainingSeconds() {
        return this.timerRemainingSeconds;
    }

    @NotNull
    public final StateFlow<Boolean> getTimerIsRunning() {
        return this.timerIsRunning;
    }

    @NotNull
    public final StateFlow<Boolean> getTimerIsPaused() {
        return this.timerIsPaused;
    }

    @NotNull
    public final StateFlow<List<Integer>> getTimerRemindersMinutes() {
        return this.timerRemindersMinutes;
    }

    @NotNull
    public final StateFlow<Set<Integer>> getTimerAlertsTriggered() {
        return this.timerAlertsTriggered;
    }

    @NotNull
    public final StateFlow<Boolean> isAlarmActive() {
        return this.isAlarmActive;
    }

    @NotNull
    public final StateFlow<Boolean> getShowTimerDialog() {
        return this.showTimerDialog;
    }

    @NotNull
    public final StateFlow<String> getAlarmSoundTitle() {
        return this.alarmSoundTitle;
    }

    @NotNull
    public final StateFlow<Float> getBtProximityRelativeAngle() {
        return this.btProximityRelativeAngle;
    }

    @NotNull
    public final StateFlow<NavigationTelemetry> getNavigationTelemetry() {
        return this.navigationTelemetry;
    }

    public final void startLocationTracking() {
        if (this.locationCallback != null) {
            return;
        }
        this.locationCallback = LocationHelper.INSTANCE.startContinuousLocationUpdates((Context)this.getApplication(), 4000L, (Function1<? super Location, Unit>)((Function1)arg_0 -> ParkingViewModel.startLocationTracking$lambda$3(this, arg_0)));
    }

    public final void stopLocationTracking() {
        block0: {
            LocationCallback locationCallback = this.locationCallback;
            if (locationCallback == null) break block0;
            LocationCallback locationCallback2 = locationCallback;
            boolean bl = false;
            LocationHelper.INSTANCE.stopContinuousLocationUpdates((Context)this.getApplication(), locationCallback2);
            this.locationCallback = null;
        }
    }

    @Nullable
    public final Object searchAddressCoordinates(@NotNull String query, @NotNull Continuation<? super Pair<Double, Double>> $completion) {
        return LocationHelper.INSTANCE.getCoordinatesFromAddress((Context)this.getApplication(), query, $completion);
    }

    @Nullable
    public final Object getAddressForCoordinates(double lat, double lng, @NotNull Continuation<? super String> $completion) {
        return LocationHelper.INSTANCE.getAddressFromCoordinates((Context)this.getApplication(), lat, lng, $completion);
    }

    public final void setThemeMode(@NotNull AppThemeMode mode) {
        Intrinsics.checkNotNullParameter((Object)((Object)mode), (String)"mode");
        this._themeMode.setValue((Object)mode);
        this.prefs.edit().putString("theme_mode", mode.name()).apply();
    }

    public final void setAutoSunTheme(boolean enabled) {
        this._autoSunTheme.setValue((Object)enabled);
        this.prefs.edit().putBoolean("auto_sun_theme", enabled).apply();
        this.updateDaytimeState();
    }

    public final void updateDaytimeState() {
        Location loc;
        Location location = loc = (Location)this._currentLocation.getValue();
        Location location2 = loc;
        SunCalculator.SunTimes sunTimes = SunCalculator.getSunTimes$default(SunCalculator.INSTANCE, location != null ? Double.valueOf(location.getLatitude()) : null, location2 != null ? Double.valueOf(location2.getLongitude()) : null, 0L, 4, null);
        this._isDaytime.setValue((Object)sunTimes.isDaytime());
    }

    public final void setOledMode(boolean enabled) {
        this._oledMode.setValue((Object)enabled);
        this.prefs.edit().putBoolean("oled_mode", enabled).apply();
    }

    public final void setDynamicColor(boolean enabled) {
        this._dynamicColor.setValue((Object)enabled);
        this.prefs.edit().putBoolean("dynamic_color", enabled).apply();
    }

    public final void setCarBadgeStyle(@NotNull CarBadgeStyle style) {
        Intrinsics.checkNotNullParameter((Object)((Object)style), (String)"style");
        this._carBadgeStyle.setValue((Object)style);
        this.prefs.edit().putString("car_badge_style", style.name()).apply();
    }

    public final void setAppLanguage(@NotNull AppLanguage language) {
        Intrinsics.checkNotNullParameter((Object)((Object)language), (String)"language");
        this._appLanguage.setValue((Object)language);
        this.prefs.edit().putString("app_language", language.name()).apply();
    }

    public final void setHapticProfile(@NotNull HapticProfile profile) {
        Intrinsics.checkNotNullParameter((Object)((Object)profile), (String)"profile");
        this._hapticProfile.setValue((Object)profile);
        this.prefs.edit().putString("haptic_profile", profile.name()).apply();
        this.compassSensorManager.setHapticProfile(profile);
        HapticHelper.INSTANCE.performClickTick((Context)this.getApplication(), profile);
    }

    public final void setWaypointHapticMode(@NotNull WaypointHapticMode mode) {
        Intrinsics.checkNotNullParameter((Object)((Object)mode), (String)"mode");
        this._waypointHapticMode.setValue((Object)mode);
        this.prefs.edit().putString("waypoint_haptic_mode", mode.name()).apply();
        this.compassSensorManager.setWaypointHapticMode(mode);
        AdaptiveWaypointHapticScheduler.playPattern$default(this.compassSensorManager.getAdaptiveHapticScheduler(), HapticPatternEvent.CONFIRMATION, mode, 0.0f, 4, null);
    }

    public final void triggerHapticTest(@NotNull HapticPatternEvent event) {
        Intrinsics.checkNotNullParameter((Object)((Object)event), (String)"event");
        AdaptiveWaypointHapticScheduler.playPattern$default(this.compassSensorManager.getAdaptiveHapticScheduler(), event, (WaypointHapticMode)((Object)this._waypointHapticMode.getValue()), 0.0f, 4, null);
    }

    public final void startWaypointHapticService(@NotNull String spotName) {
        Intrinsics.checkNotNullParameter((Object)spotName, (String)"spotName");
        WaypointHapticService.Companion.startHapticService((Context)this.getApplication(), spotName, (WaypointHapticMode)((Object)this._waypointHapticMode.getValue()));
    }

    public final void stopWaypointHapticService() {
        WaypointHapticService.Companion.stopHapticService((Context)this.getApplication());
    }

    public final void selectTab(@NotNull AppTab tab, boolean addToHistory) {
        Intrinsics.checkNotNullParameter((Object)((Object)tab), (String)"tab");
        AppTab current = (AppTab)((Object)this._selectedTab.getValue());
        if (current != tab) {
            if (addToHistory && (this.tabBackStack.isEmpty() || CollectionsKt.last(this.tabBackStack) != current)) {
                this.tabBackStack.add(current);
            }
            this._selectedTab.setValue((Object)tab);
            if (tab == AppTab.COMPASS_RADAR) {
                this.startCompass();
            }
        }
    }

    public static /* synthetic */ void selectTab$default(ParkingViewModel parkingViewModel, AppTab appTab, boolean bl, int n, Object object) {
        if ((n & 2) != 0) {
            bl = true;
        }
        parkingViewModel.selectTab(appTab, bl);
    }

    public final boolean navigateBack() {
        while (!((Collection)this.tabBackStack).isEmpty()) {
            AppTab previous = this.tabBackStack.remove(this.tabBackStack.size() - 1);
            if (previous == this._selectedTab.getValue()) continue;
            this.selectTab(previous, false);
            return true;
        }
        if (this._selectedTab.getValue() != AppTab.DASHBOARD) {
            this.selectTab(AppTab.DASHBOARD, false);
            return true;
        }
        return false;
    }

    public final boolean getCanNavigateBack() {
        return !((Collection)this.tabBackStack).isEmpty() || this._selectedTab.getValue() != AppTab.DASHBOARD;
    }

    public final void startCompass() {
        this.compassSensorManager.startListening();
    }

    public final void stopCompass() {
        this.compassSensorManager.setTargetBearing(null);
        this.compassSensorManager.cancelVibration();
        this.compassSensorManager.stopListening();
    }

    public final void recalibrateCompass() {
        this.compassSensorManager.triggerManualCalibration();
    }

    public final void dismissCompassCalibration() {
        this.compassSensorManager.dismissCalibration();
    }

    public final void refreshCurrentLocation(@Nullable Function1<? super Location, Unit> onComplete) {
        BuildersKt.launch$default((CoroutineScope)ViewModelKt.getViewModelScope((ViewModel)((ViewModel)this)), null, null, (Function2)((Function2)new Function2<CoroutineScope, Continuation<? super Unit>, Object>(this, onComplete, null){
            Object L$0;
            int label;
            final /* synthetic */ ParkingViewModel this$0;
            final /* synthetic */ Function1<Location, Unit> $onComplete;
            {
                this.this$0 = $receiver;
                this.$onComplete = $onComplete;
                super(2, $completion);
            }

            /*
             * Unable to fully structure code
             */
            public final Object invokeSuspend(Object $result) {
                var4_2 = IntrinsicsKt.getCOROUTINE_SUSPENDED();
                switch (this.label) {
                    case 0: {
                        ResultKt.throwOnFailure((Object)$result);
                        ParkingViewModel.access$get_isGpsRefreshing$p(this.this$0).setValue((Object)Boxing.boxBoolean((boolean)true));
                        this.label = 1;
                        v0 = LocationHelper.INSTANCE.getCurrentLocation((Context)this.this$0.getApplication(), (Continuation<? super Location>)((Continuation)this));
                        if (v0 == var4_2) {
                            return var4_2;
                        }
                        ** GOTO lbl14
                    }
                    case 1: {
                        ResultKt.throwOnFailure((Object)$result);
                        v0 = $result;
lbl14:
                        // 2 sources

                        if ((loc = (Location)v0) == null) ** GOTO lbl29
                        ParkingViewModel.access$get_currentLocation$p(this.this$0).setValue((Object)loc);
                        this.this$0.updateDaytimeState();
                        this.L$0 = SpillingKt.nullOutSpilledVariable((Object)loc);
                        this.label = 2;
                        v1 = LocationHelper.INSTANCE.getAddressFromCoordinates((Context)this.this$0.getApplication(), loc.getLatitude(), loc.getLongitude(), (Continuation<? super String>)((Continuation)this));
                        if (v1 == var4_2) {
                            return var4_2;
                        }
                        ** GOTO lbl27
                    }
                    case 2: {
                        loc = (Location)this.L$0;
                        ResultKt.throwOnFailure((Object)$result);
                        v1 = $result;
lbl27:
                        // 2 sources

                        addr = (String)v1;
                        ParkingViewModel.access$get_currentAddress$p(this.this$0).setValue((Object)addr);
lbl29:
                        // 2 sources

                        ParkingViewModel.access$get_isGpsRefreshing$p(this.this$0).setValue((Object)Boxing.boxBoolean((boolean)false));
                        v2 = this.$onComplete;
                        if (v2 != null) {
                            v2.invoke(ParkingViewModel.access$get_currentLocation$p(this.this$0).getValue());
                        }
                        return Unit.INSTANCE;
                    }
                }
                throw new IllegalStateException("call to 'resume' before 'invoke' with coroutine");
            }

            public final Continuation<Unit> create(Object value, Continuation<?> $completion) {
                return (Continuation)new /* invalid duplicate definition of identical inner class */;
            }

            public final Object invoke(CoroutineScope p1, Continuation<? super Unit> p2) {
                return (this.create(p1, p2)).invokeSuspend(Unit.INSTANCE);
            }
        }), (int)3, null);
    }

    public static /* synthetic */ void refreshCurrentLocation$default(ParkingViewModel parkingViewModel, Function1 function1, int n, Object object) {
        if ((n & 1) != 0) {
            function1 = null;
        }
        parkingViewModel.refreshCurrentLocation((Function1<? super Location, Unit>)function1);
    }

    public final void saveCurrentLocationAsParking(@NotNull String spotName, @NotNull String floorLevel, @NotNull String note, @Nullable Double customLat, @Nullable Double customLng, @Nullable String customAddress) {
        Intrinsics.checkNotNullParameter((Object)spotName, (String)"spotName");
        Intrinsics.checkNotNullParameter((Object)floorLevel, (String)"floorLevel");
        Intrinsics.checkNotNullParameter((Object)note, (String)"note");
        BuildersKt.launch$default((CoroutineScope)ViewModelKt.getViewModelScope((ViewModel)((ViewModel)this)), null, null, (Function2)((Function2)new Function2<CoroutineScope, Continuation<? super Unit>, Object>(this, customLat, customLng, customAddress, spotName, floorLevel, note, null){
            Object L$0;
            Object L$1;
            Object L$2;
            Object L$3;
            Object L$4;
            Object L$5;
            double D$0;
            float F$0;
            long J$0;
            int label;
            final /* synthetic */ ParkingViewModel this$0;
            final /* synthetic */ Double $customLat;
            final /* synthetic */ Double $customLng;
            final /* synthetic */ String $customAddress;
            final /* synthetic */ String $spotName;
            final /* synthetic */ String $floorLevel;
            final /* synthetic */ String $note;
            {
                this.this$0 = $receiver;
                this.$customLat = $customLat;
                this.$customLng = $customLng;
                this.$customAddress = $customAddress;
                this.$spotName = $spotName;
                this.$floorLevel = $floorLevel;
                this.$note = $note;
                super(2, $completion);
            }

            /*
             * Unable to fully structure code
             */
            public final Object invokeSuspend(Object $result) {
                block20: {
                    var46_2 = IntrinsicsKt.getCOROUTINE_SUSPENDED();
                    switch (this.label) {
                        case 0: {
                            ResultKt.throwOnFailure((Object)$result);
                            ParkingViewModel.access$get_isGpsRefreshing$p(this.this$0).setValue((Object)Boxing.boxBoolean((boolean)true));
                            if (this.$customLat == null || this.$customLng == null) ** GOTO lbl9
                            v0 = null;
                            ** GOTO lbl20
lbl9:
                            // 1 sources

                            v0 = (Location)ParkingViewModel.access$get_currentLocation$p(this.this$0).getValue();
                            if (v0 != null) ** GOTO lbl20
                            this.label = 1;
                            v1 = LocationHelper.INSTANCE.getCurrentLocation((Context)this.this$0.getApplication(), (Continuation<? super Location>)((Continuation)this));
                            if (v1 == var46_2) {
                                return var46_2;
                            }
                            ** GOTO lbl19
                        }
                        case 1: {
                            ResultKt.throwOnFailure((Object)$result);
                            v1 = $result;
lbl19:
                            // 2 sources

                            v0 = loc = (Location)v1;
lbl20:
                            // 3 sources

                            if ((v2 = this.$customLat) == null) {
                                v3 = loc;
                                v2 = lat = v3 != null ? Boxing.boxDouble((double)v3.getLatitude()) : null;
                            }
                            if ((v4 = this.$customLng) == null) {
                                v5 = loc;
                                v4 = lng = v5 != null ? Boxing.boxDouble((double)v5.getLongitude()) : null;
                            }
                            if (lat != null && lng != null) break;
                            ParkingViewModel.access$get_isGpsRefreshing$p(this.this$0).setValue((Object)Boxing.boxBoolean((boolean)false));
                            this.L$0 = SpillingKt.nullOutSpilledVariable((Object)loc);
                            this.L$1 = SpillingKt.nullOutSpilledVariable((Object)lat);
                            this.L$2 = SpillingKt.nullOutSpilledVariable((Object)lng);
                            this.label = 2;
                            v6 = BuildersKt.withContext((CoroutineContext)((CoroutineContext)Dispatchers.getMain()), (Function2)((Function2)new Function2<CoroutineScope, Continuation<? super Unit>, Object>(this.this$0, null){
                                int label;
                                final /* synthetic */ ParkingViewModel this$0;
                                {
                                    this.this$0 = $receiver;
                                    super(2, $completion);
                                }

                                public final Object invokeSuspend(Object $result) {
                                    IntrinsicsKt.getCOROUTINE_SUSPENDED();
                                    switch (this.label) {
                                        case 0: {
                                            ResultKt.throwOnFailure((Object)$result);
                                            ParkingNotificationHelper.INSTANCE.showSystemNotification((Context)this.this$0.getApplication(), "Pixel Parking", "Acquiring GPS location... Please ensure Location is enabled, or search for your address.");
                                            return Unit.INSTANCE;
                                        }
                                    }
                                    throw new IllegalStateException("call to 'resume' before 'invoke' with coroutine");
                                }

                                public final Continuation<Unit> create(Object value, Continuation<?> $completion) {
                                    return (Continuation)new /* invalid duplicate definition of identical inner class */;
                                }

                                public final Object invoke(CoroutineScope p1, Continuation<? super Unit> p2) {
                                    return (this.create(p1, p2)).invokeSuspend(Unit.INSTANCE);
                                }
                            }), (Continuation)((Continuation)this));
                            if (v6 == var46_2) {
                                return var46_2;
                            }
                            ** GOTO lbl42
                        }
                        case 2: {
                            lng = (Double)this.L$2;
                            lat = (Double)this.L$1;
                            loc = (Location)this.L$0;
                            ResultKt.throwOnFailure((Object)$result);
                            v6 = $result;
lbl42:
                            // 2 sources

                            return Unit.INSTANCE;
                        }
                    }
                    v7 = loc;
                    alt = v7 != null ? v7.getAltitude() : 0.0;
                    v8 = loc;
                    accuracy = v8 != null ? v8.getAccuracy() : 0.0f;
                    var9_16 = this.$customAddress;
                    if (var9_16 == null || StringsKt.isBlank((CharSequence)var9_16) != false) break block20;
                    v9 = this.$customAddress;
                    ** GOTO lbl71
                }
                this.L$0 = loc;
                this.L$1 = lat;
                this.L$2 = lng;
                this.D$0 = alt;
                this.F$0 = accuracy;
                this.label = 3;
                v10 = LocationHelper.INSTANCE.getAddressFromCoordinates((Context)this.this$0.getApplication(), lat, lng, (Continuation<? super String>)((Continuation)this));
                if (v10 == var46_2) {
                    return var46_2;
                }
                ** GOTO lbl70
                {
                    case 3: {
                        accuracy = this.F$0;
                        alt = this.D$0;
                        lng = (Double)this.L$2;
                        lat = (Double)this.L$1;
                        loc = (Location)this.L$0;
                        ResultKt.throwOnFailure((Object)$result);
                        v10 = $result;
lbl70:
                        // 2 sources

                        v9 = (String)v10;
lbl71:
                        // 2 sources

                        address = v9;
                        v11 = 0L;
                        v12 = lat;
                        v13 = lng;
                        v14 = alt;
                        v15 = accuracy;
                        v16 = address;
                        var10_20 = this.$spotName;
                        if (StringsKt.isBlank((CharSequence)var10_20)) {
                            var22_23 = v16;
                            var21_24 = v15;
                            var19_25 = v14;
                            var17_26 = v13;
                            var15_27 = v12;
                            var13_28 = v11;
                            $i$a$-ifBlank-ParkingViewModel$saveCurrentLocationAsParking$1$spot$1\1\496\0 = false;
                            var23_30 = "Parked Car";
                            v11 = var13_28;
                            v12 = var15_27;
                            v13 = var17_26;
                            v14 = var19_25;
                            v15 = var21_24;
                            v16 = var22_23;
                            v17 = var23_30;
                        } else {
                            v17 = var10_20;
                        }
                        v18 = (String)v17;
                        var10_20 = this.$floorLevel;
                        if (StringsKt.isBlank((CharSequence)var10_20)) {
                            var23_30 = v18;
                            var22_23 = v16;
                            var21_24 = v15;
                            var19_25 = v14;
                            var17_26 = v13;
                            var15_27 = v12;
                            var13_28 = v11;
                            $i$a$-ifBlank-ParkingViewModel$saveCurrentLocationAsParking$1$spot$2\2\497\0 = false;
                            var24_31 = "Ground Level";
                            v11 = var13_28;
                            v12 = var15_27;
                            v13 = var17_26;
                            v14 = var19_25;
                            v15 = var21_24;
                            v16 = var22_23;
                            v18 = var23_30;
                            v19 = var24_31;
                        } else {
                            v19 = var10_20;
                        }
                        var25_32 = null;
                        var26_33 = 25089;
                        var27_34 = null;
                        var28_35 = null;
                        var29_36 = this.$customLat != null ? "Custom Location" : "Manual GPS Entry";
                        var30_37 = System.currentTimeMillis();
                        var32_38 = null;
                        var33_39 = this.$note;
                        var34_40 = (String)v19;
                        var35_41 = v18;
                        var36_42 = v16;
                        var37_43 = v15;
                        var38_44 = v14;
                        var40_45 = v13;
                        var42_46 = v12;
                        var44_47 = v11;
                        spot = new ParkingSpot(var44_47, var42_46, var40_45, var38_44, var37_43, var36_42, var35_41, var34_40, var33_39, var32_38, var30_37, false, var29_36, var28_35, var27_34, true, var26_33, var25_32);
                        this.L$0 = loc;
                        this.L$1 = SpillingKt.nullOutSpilledVariable((Object)lat);
                        this.L$2 = SpillingKt.nullOutSpilledVariable((Object)lng);
                        this.L$3 = address;
                        this.L$4 = spot;
                        this.D$0 = alt;
                        this.F$0 = accuracy;
                        this.label = 4;
                        v20 = ParkingViewModel.access$getRepository$p(this.this$0).saveNewParkingSpot(spot, (Continuation<? super Long>)((Continuation)this));
                        if (v20 == var46_2) {
                            return var46_2;
                        }
                        ** GOTO lbl159
                    }
                    case 4: {
                        accuracy = this.F$0;
                        alt = this.D$0;
                        spot = (ParkingSpot)this.L$4;
                        address = (String)this.L$3;
                        lng = (Double)this.L$2;
                        lat = (Double)this.L$1;
                        loc = (Location)this.L$0;
                        ResultKt.throwOnFailure((Object)$result);
                        v20 = $result;
lbl159:
                        // 2 sources

                        id = ((Number)v20).longValue();
                        saved = ParkingSpot.copy$default(spot, id, 0.0, 0.0, 0.0, 0.0f, null, null, null, null, null, 0L, false, null, null, null, false, 65534, null);
                        ParkingViewModel.access$get_currentAddress$p(this.this$0).setValue((Object)address);
                        if (loc != null) {
                            ParkingViewModel.access$get_currentLocation$p(this.this$0).setValue((Object)loc);
                        }
                        ParkingNotificationHelper.INSTANCE.showCarParkedNotification((Context)this.this$0.getApplication(), saved);
                        ParkingViewModel.access$get_isGpsRefreshing$p(this.this$0).setValue((Object)Boxing.boxBoolean((boolean)false));
                        this.L$0 = SpillingKt.nullOutSpilledVariable((Object)loc);
                        this.L$1 = SpillingKt.nullOutSpilledVariable((Object)lat);
                        this.L$2 = SpillingKt.nullOutSpilledVariable((Object)lng);
                        this.L$3 = SpillingKt.nullOutSpilledVariable((Object)address);
                        this.L$4 = SpillingKt.nullOutSpilledVariable((Object)spot);
                        this.L$5 = SpillingKt.nullOutSpilledVariable((Object)saved);
                        this.D$0 = alt;
                        this.F$0 = accuracy;
                        this.J$0 = id;
                        this.label = 5;
                        v21 = BuildersKt.withContext((CoroutineContext)((CoroutineContext)Dispatchers.getMain()), (Function2)((Function2)new Function2<CoroutineScope, Continuation<? super Unit>, Object>(this.this$0, address, null){
                            int label;
                            final /* synthetic */ ParkingViewModel this$0;
                            final /* synthetic */ String $address;
                            {
                                this.this$0 = $receiver;
                                this.$address = $address;
                                super(2, $completion);
                            }

                            public final Object invokeSuspend(Object $result) {
                                IntrinsicsKt.getCOROUTINE_SUSPENDED();
                                switch (this.label) {
                                    case 0: {
                                        ResultKt.throwOnFailure((Object)$result);
                                        ParkingNotificationHelper.INSTANCE.showSystemNotification((Context)this.this$0.getApplication(), "Pixel Parking", "Saved actual location: " + this.$address);
                                        return Unit.INSTANCE;
                                    }
                                }
                                throw new IllegalStateException("call to 'resume' before 'invoke' with coroutine");
                            }

                            public final Continuation<Unit> create(Object value, Continuation<?> $completion) {
                                return (Continuation)new /* invalid duplicate definition of identical inner class */;
                            }

                            public final Object invoke(CoroutineScope p1, Continuation<? super Unit> p2) {
                                return (this.create(p1, p2)).invokeSuspend(Unit.INSTANCE);
                            }
                        }), (Continuation)((Continuation)this));
                        if (v21 == var46_2) {
                            return var46_2;
                        }
                        ** GOTO lbl192
                    }
                    case 5: {
                        id = this.J$0;
                        accuracy = this.F$0;
                        alt = this.D$0;
                        saved = (ParkingSpot)this.L$5;
                        spot = (ParkingSpot)this.L$4;
                        address = (String)this.L$3;
                        lng = (Double)this.L$2;
                        lat = (Double)this.L$1;
                        loc = (Location)this.L$0;
                        ResultKt.throwOnFailure((Object)$result);
                        v21 = $result;
lbl192:
                        // 2 sources

                        return Unit.INSTANCE;
                    }
                }
                throw new IllegalStateException("call to 'resume' before 'invoke' with coroutine");
            }

            public final Continuation<Unit> create(Object value, Continuation<?> $completion) {
                return (Continuation)new /* invalid duplicate definition of identical inner class */;
            }

            public final Object invoke(CoroutineScope p1, Continuation<? super Unit> p2) {
                return (this.create(p1, p2)).invokeSuspend(Unit.INSTANCE);
            }
        }), (int)3, null);
    }

    public static /* synthetic */ void saveCurrentLocationAsParking$default(ParkingViewModel parkingViewModel, String string, String string2, String string3, Double d, Double d2, String string4, int n, Object object) {
        if ((n & 1) != 0) {
            string = "My Parked Car";
        }
        if ((n & 2) != 0) {
            string2 = "Ground Level";
        }
        if ((n & 4) != 0) {
            string3 = "";
        }
        if ((n & 8) != 0) {
            d = null;
        }
        if ((n & 0x10) != 0) {
            d2 = null;
        }
        if ((n & 0x20) != 0) {
            string4 = null;
        }
        parkingViewModel.saveCurrentLocationAsParking(string, string2, string3, d, d2, string4);
    }

    public final void updateSpotDetails(long id, @NotNull String newName, @NotNull String newFloor, @NotNull String newNote, @Nullable Integer meterMinutes) {
        Intrinsics.checkNotNullParameter((Object)newName, (String)"newName");
        Intrinsics.checkNotNullParameter((Object)newFloor, (String)"newFloor");
        Intrinsics.checkNotNullParameter((Object)newNote, (String)"newNote");
        BuildersKt.launch$default((CoroutineScope)ViewModelKt.getViewModelScope((ViewModel)((ViewModel)this)), null, null, (Function2)((Function2)new Function2<CoroutineScope, Continuation<? super Unit>, Object>(this, id, meterMinutes, newName, newFloor, newNote, null){
            Object L$0;
            Object L$1;
            Object L$2;
            int label;
            final /* synthetic */ ParkingViewModel this$0;
            final /* synthetic */ long $id;
            final /* synthetic */ Integer $meterMinutes;
            final /* synthetic */ String $newName;
            final /* synthetic */ String $newFloor;
            final /* synthetic */ String $newNote;
            {
                this.this$0 = $receiver;
                this.$id = $id;
                this.$meterMinutes = $meterMinutes;
                this.$newName = $newName;
                this.$newFloor = $newFloor;
                this.$newNote = $newNote;
                super(2, $completion);
            }

            /*
             * Enabled force condition propagation
             * Lifted jumps to return sites
             */
            public final Object invokeSuspend(Object $result) {
                Object object = IntrinsicsKt.getCOROUTINE_SUSPENDED();
                switch (this.label) {
                    case 0: {
                        Long l;
                        ResultKt.throwOnFailure((Object)$result);
                        ParkingSpot current = (ParkingSpot)this.this$0.getActiveSpot().getValue();
                        if (current == null || current.getId() != this.$id) return Unit.INSTANCE;
                        Integer n = this.$meterMinutes;
                        if (n != null) {
                            int n2 = ((Number)n).intValue();
                            boolean bl = false;
                            l = Boxing.boxLong((long)(System.currentTimeMillis() + (long)(n2 * 60) * 1000L));
                        } else {
                            l = current.getMeterExpiryTimestamp();
                        }
                        Long meterExpiry = l;
                        ParkingSpot updated = ParkingSpot.copy$default(current, 0L, 0.0, 0.0, 0.0, 0.0f, null, this.$newName, this.$newFloor, this.$newNote, null, 0L, false, null, null, meterExpiry, false, 48703, null);
                        this.L$0 = SpillingKt.nullOutSpilledVariable((Object)current);
                        this.L$1 = SpillingKt.nullOutSpilledVariable((Object)meterExpiry);
                        this.L$2 = SpillingKt.nullOutSpilledVariable((Object)updated);
                        this.label = 1;
                        Object object2 = ParkingViewModel.access$getRepository$p(this.this$0).updateParkingSpot(updated, (Continuation<? super Unit>)((Continuation)this));
                        if (object2 != object) return Unit.INSTANCE;
                        return object;
                    }
                    case 1: {
                        ParkingSpot updated = (ParkingSpot)this.L$2;
                        Long meterExpiry = (Long)this.L$1;
                        ParkingSpot current = (ParkingSpot)this.L$0;
                        ResultKt.throwOnFailure((Object)$result);
                        Object object2 = $result;
                        return Unit.INSTANCE;
                    }
                }
                throw new IllegalStateException("call to 'resume' before 'invoke' with coroutine");
            }

            public final Continuation<Unit> create(Object value, Continuation<?> $completion) {
                return (Continuation)new /* invalid duplicate definition of identical inner class */;
            }

            public final Object invoke(CoroutineScope p1, Continuation<? super Unit> p2) {
                return (this.create(p1, p2)).invokeSuspend(Unit.INSTANCE);
            }
        }), (int)3, null);
    }

    public static /* synthetic */ void updateSpotDetails$default(ParkingViewModel parkingViewModel, long l, String string, String string2, String string3, Integer n, int n2, Object object) {
        if ((n2 & 0x10) != 0) {
            n = null;
        }
        parkingViewModel.updateSpotDetails(l, string, string2, string3, n);
    }

    public final void updateSpotNote(long spotId, @NotNull String newNote) {
        Intrinsics.checkNotNullParameter((Object)newNote, (String)"newNote");
        BuildersKt.launch$default((CoroutineScope)ViewModelKt.getViewModelScope((ViewModel)((ViewModel)this)), null, null, (Function2)((Function2)new Function2<CoroutineScope, Continuation<? super Unit>, Object>(this, spotId, newNote, null){
            int label;
            final /* synthetic */ ParkingViewModel this$0;
            final /* synthetic */ long $spotId;
            final /* synthetic */ String $newNote;
            {
                this.this$0 = $receiver;
                this.$spotId = $spotId;
                this.$newNote = $newNote;
                super(2, $completion);
            }

            /*
             * Enabled force condition propagation
             * Lifted jumps to return sites
             */
            public final Object invokeSuspend(Object $result) {
                Object object = IntrinsicsKt.getCOROUTINE_SUSPENDED();
                switch (this.label) {
                    case 0: {
                        ResultKt.throwOnFailure((Object)$result);
                        this.label = 1;
                        Object object2 = ParkingViewModel.access$getRepository$p(this.this$0).updateSpotNote(this.$spotId, this.$newNote, (Continuation<? super Unit>)((Continuation)this));
                        if (object2 != object) return Unit.INSTANCE;
                        return object;
                    }
                    case 1: {
                        ResultKt.throwOnFailure((Object)$result);
                        Object object2 = $result;
                        return Unit.INSTANCE;
                    }
                }
                throw new IllegalStateException("call to 'resume' before 'invoke' with coroutine");
            }

            public final Continuation<Unit> create(Object value, Continuation<?> $completion) {
                return (Continuation)new /* invalid duplicate definition of identical inner class */;
            }

            public final Object invoke(CoroutineScope p1, Continuation<? super Unit> p2) {
                return (this.create(p1, p2)).invokeSuspend(Unit.INSTANCE);
            }
        }), (int)3, null);
    }

    public final void updateHistorySpot(long id, @NotNull String name, @NotNull String floor, @NotNull String note, @Nullable Double customLat, @Nullable Double customLng, @Nullable String customAddress) {
        Intrinsics.checkNotNullParameter((Object)name, (String)"name");
        Intrinsics.checkNotNullParameter((Object)floor, (String)"floor");
        Intrinsics.checkNotNullParameter((Object)note, (String)"note");
        BuildersKt.launch$default((CoroutineScope)ViewModelKt.getViewModelScope((ViewModel)((ViewModel)this)), null, null, (Function2)((Function2)new Function2<CoroutineScope, Continuation<? super Unit>, Object>(this, id, customLat, customLng, customAddress, name, floor, note, null){
            Object L$0;
            Object L$1;
            int label;
            final /* synthetic */ ParkingViewModel this$0;
            final /* synthetic */ long $id;
            final /* synthetic */ Double $customLat;
            final /* synthetic */ Double $customLng;
            final /* synthetic */ String $customAddress;
            final /* synthetic */ String $name;
            final /* synthetic */ String $floor;
            final /* synthetic */ String $note;
            {
                this.this$0 = $receiver;
                this.$id = $id;
                this.$customLat = $customLat;
                this.$customLng = $customLng;
                this.$customAddress = $customAddress;
                this.$name = $name;
                this.$floor = $floor;
                this.$note = $note;
                super(2, $completion);
            }

            /*
             * Unable to fully structure code
             */
            public final Object invokeSuspend(Object $result) {
                var9_2 = IntrinsicsKt.getCOROUTINE_SUSPENDED();
                switch (this.label) {
                    case 0: {
                        ResultKt.throwOnFailure((Object)$result);
                        this.label = 1;
                        v0 = ParkingViewModel.access$getRepository$p(this.this$0).getSpotById(this.$id, (Continuation<? super ParkingSpot>)((Continuation)this));
                        if (v0 == var9_2) {
                            return var9_2;
                        }
                        ** GOTO lbl13
                    }
                    case 1: {
                        ResultKt.throwOnFailure((Object)$result);
                        v0 = $result;
lbl13:
                        // 2 sources

                        if ((spot = (ParkingSpot)v0) != null) {
                            v1 = this.$customLat;
                            var4_5 = v1 != null ? v1.doubleValue() : spot.getLatitude();
                            v2 = this.$customLng;
                            var6_6 = v2 != null ? v2.doubleValue() : spot.getLongitude();
                            v3 = this.$customAddress;
                            if (v3 == null) {
                                v3 = spot.getAddress();
                            }
                            var8_7 = v3;
                            updated = ParkingSpot.copy$default(spot, 0L, var4_5, var6_6, 0.0, 0.0f, var8_7, this.$name, this.$floor, this.$note, null, 0L, false, null, null, null, false, 65049, null);
                            this.L$0 = SpillingKt.nullOutSpilledVariable((Object)spot);
                            this.L$1 = SpillingKt.nullOutSpilledVariable((Object)updated);
                            this.label = 2;
                            v4 = ParkingViewModel.access$getRepository$p(this.this$0).updateParkingSpot(updated, (Continuation<? super Unit>)((Continuation)this));
                            if (v4 == var9_2) {
                                return var9_2;
                            }
                        }
                        ** GOTO lbl35
                    }
                    case 2: {
                        updated = (ParkingSpot)this.L$1;
                        spot = (ParkingSpot)this.L$0;
                        ResultKt.throwOnFailure((Object)$result);
                        v4 = $result;
lbl35:
                        // 2 sources

                        return Unit.INSTANCE;
                    }
                }
                throw new IllegalStateException("call to 'resume' before 'invoke' with coroutine");
            }

            public final Continuation<Unit> create(Object value, Continuation<?> $completion) {
                return (Continuation)new /* invalid duplicate definition of identical inner class */;
            }

            public final Object invoke(CoroutineScope p1, Continuation<? super Unit> p2) {
                return (this.create(p1, p2)).invokeSuspend(Unit.INSTANCE);
            }
        }), (int)3, null);
    }

    public static /* synthetic */ void updateHistorySpot$default(ParkingViewModel parkingViewModel, long l, String string, String string2, String string3, Double d, Double d2, String string4, int n, Object object) {
        if ((n & 0x10) != 0) {
            d = null;
        }
        if ((n & 0x20) != 0) {
            d2 = null;
        }
        if ((n & 0x40) != 0) {
            string4 = null;
        }
        parkingViewModel.updateHistorySpot(l, string, string2, string3, d, d2, string4);
    }

    public final void setActiveNavigationTarget(@NotNull ParkingSpot spot) {
        Intrinsics.checkNotNullParameter((Object)spot, (String)"spot");
        BuildersKt.launch$default((CoroutineScope)ViewModelKt.getViewModelScope((ViewModel)((ViewModel)this)), null, null, (Function2)((Function2)new Function2<CoroutineScope, Continuation<? super Unit>, Object>(this, spot, null){
            int label;
            final /* synthetic */ ParkingViewModel this$0;
            final /* synthetic */ ParkingSpot $spot;
            {
                this.this$0 = $receiver;
                this.$spot = $spot;
                super(2, $completion);
            }

            /*
             * Unable to fully structure code
             */
            public final Object invokeSuspend(Object $result) {
                var2_2 = IntrinsicsKt.getCOROUTINE_SUSPENDED();
                switch (this.label) {
                    case 0: {
                        ResultKt.throwOnFailure((Object)$result);
                        this.label = 1;
                        v0 = ParkingViewModel.access$getRepository$p(this.this$0).setActiveSpot(this.$spot.getId(), (Continuation<? super Unit>)((Continuation)this));
                        if (v0 == var2_2) {
                            return var2_2;
                        }
                        ** GOTO lbl13
                    }
                    case 1: {
                        ResultKt.throwOnFailure((Object)$result);
                        v0 = $result;
lbl13:
                        // 2 sources

                        ParkingViewModel.access$get_selectedTab$p(this.this$0).setValue((Object)AppTab.COMPASS_RADAR);
                        this.this$0.startCompass();
                        this.label = 2;
                        v1 = BuildersKt.withContext((CoroutineContext)((CoroutineContext)Dispatchers.getMain()), (Function2)((Function2)new Function2<CoroutineScope, Continuation<? super Unit>, Object>(this.this$0, this.$spot, null){
                            int label;
                            final /* synthetic */ ParkingViewModel this$0;
                            final /* synthetic */ ParkingSpot $spot;
                            {
                                this.this$0 = $receiver;
                                this.$spot = $spot;
                                super(2, $completion);
                            }

                            public final Object invokeSuspend(Object $result) {
                                IntrinsicsKt.getCOROUTINE_SUSPENDED();
                                switch (this.label) {
                                    case 0: {
                                        ResultKt.throwOnFailure((Object)$result);
                                        ParkingNotificationHelper.INSTANCE.showSystemNotification((Context)this.this$0.getApplication(), "Pixel Parking", "Navigating to: " + this.$spot.getSpotName());
                                        return Unit.INSTANCE;
                                    }
                                }
                                throw new IllegalStateException("call to 'resume' before 'invoke' with coroutine");
                            }

                            public final Continuation<Unit> create(Object value, Continuation<?> $completion) {
                                return (Continuation)new /* invalid duplicate definition of identical inner class */;
                            }

                            public final Object invoke(CoroutineScope p1, Continuation<? super Unit> p2) {
                                return (this.create(p1, p2)).invokeSuspend(Unit.INSTANCE);
                            }
                        }), (Continuation)((Continuation)this));
                        if (v1 == var2_2) {
                            return var2_2;
                        }
                        ** GOTO lbl23
                    }
                    case 2: {
                        ResultKt.throwOnFailure((Object)$result);
                        v1 = $result;
lbl23:
                        // 2 sources

                        return Unit.INSTANCE;
                    }
                }
                throw new IllegalStateException("call to 'resume' before 'invoke' with coroutine");
            }

            public final Continuation<Unit> create(Object value, Continuation<?> $completion) {
                return (Continuation)new /* invalid duplicate definition of identical inner class */;
            }

            public final Object invoke(CoroutineScope p1, Continuation<? super Unit> p2) {
                return (this.create(p1, p2)).invokeSuspend(Unit.INSTANCE);
            }
        }), (int)3, null);
    }

    public final void setParkingMeter(int minutes) {
        BuildersKt.launch$default((CoroutineScope)ViewModelKt.getViewModelScope((ViewModel)((ViewModel)this)), null, null, (Function2)((Function2)new Function2<CoroutineScope, Continuation<? super Unit>, Object>(this, minutes, null){
            Object L$0;
            long J$0;
            int label;
            final /* synthetic */ ParkingViewModel this$0;
            final /* synthetic */ int $minutes;
            {
                this.this$0 = $receiver;
                this.$minutes = $minutes;
                super(2, $completion);
            }

            /*
             * Unable to fully structure code
             */
            public final Object invokeSuspend(Object $result) {
                var5_2 = IntrinsicsKt.getCOROUTINE_SUSPENDED();
                switch (this.label) {
                    case 0: {
                        ResultKt.throwOnFailure((Object)$result);
                        v0 = (ParkingSpot)this.this$0.getActiveSpot().getValue();
                        if (v0 == null) {
                            return Unit.INSTANCE;
                        }
                        current = v0;
                        expiry = System.currentTimeMillis() + (long)(this.$minutes * 60) * 1000L;
                        this.L$0 = SpillingKt.nullOutSpilledVariable((Object)current);
                        this.J$0 = expiry;
                        this.label = 1;
                        v1 = ParkingViewModel.access$getRepository$p(this.this$0).updateParkingSpot(ParkingSpot.copy$default(current, 0L, 0.0, 0.0, 0.0, 0.0f, null, null, null, null, null, 0L, false, null, null, Boxing.boxLong((long)expiry), false, 49151, null), (Continuation<? super Unit>)((Continuation)this));
                        if (v1 == var5_2) {
                            return var5_2;
                        }
                        ** GOTO lbl22
                    }
                    case 1: {
                        expiry = this.J$0;
                        current = (ParkingSpot)this.L$0;
                        ResultKt.throwOnFailure((Object)$result);
                        v1 = $result;
lbl22:
                        // 2 sources

                        this.L$0 = SpillingKt.nullOutSpilledVariable((Object)current);
                        this.J$0 = expiry;
                        this.label = 2;
                        v2 = BuildersKt.withContext((CoroutineContext)((CoroutineContext)Dispatchers.getMain()), (Function2)((Function2)new Function2<CoroutineScope, Continuation<? super Unit>, Object>(this.this$0, this.$minutes, null){
                            int label;
                            final /* synthetic */ ParkingViewModel this$0;
                            final /* synthetic */ int $minutes;
                            {
                                this.this$0 = $receiver;
                                this.$minutes = $minutes;
                                super(2, $completion);
                            }

                            public final Object invokeSuspend(Object $result) {
                                IntrinsicsKt.getCOROUTINE_SUSPENDED();
                                switch (this.label) {
                                    case 0: {
                                        ResultKt.throwOnFailure((Object)$result);
                                        ParkingNotificationHelper.INSTANCE.showSystemNotification((Context)this.this$0.getApplication(), "Pixel Parking", "Meter alarm set for " + this.$minutes + " mins");
                                        return Unit.INSTANCE;
                                    }
                                }
                                throw new IllegalStateException("call to 'resume' before 'invoke' with coroutine");
                            }

                            public final Continuation<Unit> create(Object value, Continuation<?> $completion) {
                                return (Continuation)new /* invalid duplicate definition of identical inner class */;
                            }

                            public final Object invoke(CoroutineScope p1, Continuation<? super Unit> p2) {
                                return (this.create(p1, p2)).invokeSuspend(Unit.INSTANCE);
                            }
                        }), (Continuation)((Continuation)this));
                        if (v2 == var5_2) {
                            return var5_2;
                        }
                        ** GOTO lbl34
                    }
                    case 2: {
                        expiry = this.J$0;
                        current = (ParkingSpot)this.L$0;
                        ResultKt.throwOnFailure((Object)$result);
                        v2 = $result;
lbl34:
                        // 2 sources

                        return Unit.INSTANCE;
                    }
                }
                throw new IllegalStateException("call to 'resume' before 'invoke' with coroutine");
            }

            public final Continuation<Unit> create(Object value, Continuation<?> $completion) {
                return (Continuation)new /* invalid duplicate definition of identical inner class */;
            }

            public final Object invoke(CoroutineScope p1, Continuation<? super Unit> p2) {
                return (this.create(p1, p2)).invokeSuspend(Unit.INSTANCE);
            }
        }), (int)3, null);
    }

    public final void markCarFound() {
        BuildersKt.launch$default((CoroutineScope)ViewModelKt.getViewModelScope((ViewModel)((ViewModel)this)), null, null, (Function2)((Function2)new Function2<CoroutineScope, Continuation<? super Unit>, Object>(this, null){
            Object L$0;
            int label;
            final /* synthetic */ ParkingViewModel this$0;
            {
                this.this$0 = $receiver;
                super(2, $completion);
            }

            /*
             * Unable to fully structure code
             */
            public final Object invokeSuspend(Object $result) {
                var3_2 = IntrinsicsKt.getCOROUTINE_SUSPENDED();
                switch (this.label) {
                    case 0: {
                        ResultKt.throwOnFailure((Object)$result);
                        v0 = (ParkingSpot)this.this$0.getActiveSpot().getValue();
                        if (v0 == null) {
                            return Unit.INSTANCE;
                        }
                        current = v0;
                        this.L$0 = SpillingKt.nullOutSpilledVariable((Object)current);
                        this.label = 1;
                        v1 = ParkingViewModel.access$getRepository$p(this.this$0).updateParkingSpot(ParkingSpot.copy$default(current, 0L, 0.0, 0.0, 0.0, 0.0f, null, null, null, null, null, 0L, false, null, null, null, false, 32767, null), (Continuation<? super Unit>)((Continuation)this));
                        if (v1 == var3_2) {
                            return var3_2;
                        }
                        ** GOTO lbl19
                    }
                    case 1: {
                        current = (ParkingSpot)this.L$0;
                        ResultKt.throwOnFailure((Object)$result);
                        v1 = $result;
lbl19:
                        // 2 sources

                        ParkingViewModel.access$getCompassSensorManager$p(this.this$0).setTargetBearing(null);
                        ParkingViewModel.access$getCompassSensorManager$p(this.this$0).cancelVibration();
                        this.this$0.stopRadarService();
                        HapticHelper.INSTANCE.performConfirmationHaptic((Context)this.this$0.getApplication(), (HapticProfile)ParkingViewModel.access$get_hapticProfile$p(this.this$0).getValue());
                        return Unit.INSTANCE;
                    }
                }
                throw new IllegalStateException("call to 'resume' before 'invoke' with coroutine");
            }

            public final Continuation<Unit> create(Object value, Continuation<?> $completion) {
                return (Continuation)new /* invalid duplicate definition of identical inner class */;
            }

            public final Object invoke(CoroutineScope p1, Continuation<? super Unit> p2) {
                return (this.create(p1, p2)).invokeSuspend(Unit.INSTANCE);
            }
        }), (int)3, null);
    }

    public final void deleteSpot(long id) {
        BuildersKt.launch$default((CoroutineScope)ViewModelKt.getViewModelScope((ViewModel)((ViewModel)this)), null, null, (Function2)((Function2)new Function2<CoroutineScope, Continuation<? super Unit>, Object>(this, id, null){
            int label;
            final /* synthetic */ ParkingViewModel this$0;
            final /* synthetic */ long $id;
            {
                this.this$0 = $receiver;
                this.$id = $id;
                super(2, $completion);
            }

            /*
             * Enabled force condition propagation
             * Lifted jumps to return sites
             */
            public final Object invokeSuspend(Object $result) {
                Object object = IntrinsicsKt.getCOROUTINE_SUSPENDED();
                switch (this.label) {
                    case 0: {
                        ResultKt.throwOnFailure((Object)$result);
                        this.label = 1;
                        Object object2 = ParkingViewModel.access$getRepository$p(this.this$0).deleteSpot(this.$id, (Continuation<? super Unit>)((Continuation)this));
                        if (object2 != object) return Unit.INSTANCE;
                        return object;
                    }
                    case 1: {
                        ResultKt.throwOnFailure((Object)$result);
                        Object object2 = $result;
                        return Unit.INSTANCE;
                    }
                }
                throw new IllegalStateException("call to 'resume' before 'invoke' with coroutine");
            }

            public final Continuation<Unit> create(Object value, Continuation<?> $completion) {
                return (Continuation)new /* invalid duplicate definition of identical inner class */;
            }

            public final Object invoke(CoroutineScope p1, Continuation<? super Unit> p2) {
                return (this.create(p1, p2)).invokeSuspend(Unit.INSTANCE);
            }
        }), (int)3, null);
    }

    public final void clearHistory() {
        BuildersKt.launch$default((CoroutineScope)ViewModelKt.getViewModelScope((ViewModel)((ViewModel)this)), null, null, (Function2)((Function2)new Function2<CoroutineScope, Continuation<? super Unit>, Object>(this, null){
            int label;
            final /* synthetic */ ParkingViewModel this$0;
            {
                this.this$0 = $receiver;
                super(2, $completion);
            }

            /*
             * Enabled force condition propagation
             * Lifted jumps to return sites
             */
            public final Object invokeSuspend(Object $result) {
                Object object = IntrinsicsKt.getCOROUTINE_SUSPENDED();
                switch (this.label) {
                    case 0: {
                        ResultKt.throwOnFailure((Object)$result);
                        this.label = 1;
                        Object object2 = ParkingViewModel.access$getRepository$p(this.this$0).clearHistory((Continuation<? super Unit>)((Continuation)this));
                        if (object2 != object) return Unit.INSTANCE;
                        return object;
                    }
                    case 1: {
                        ResultKt.throwOnFailure((Object)$result);
                        Object object2 = $result;
                        return Unit.INSTANCE;
                    }
                }
                throw new IllegalStateException("call to 'resume' before 'invoke' with coroutine");
            }

            public final Continuation<Unit> create(Object value, Continuation<?> $completion) {
                return (Continuation)new /* invalid duplicate definition of identical inner class */;
            }

            public final Object invoke(CoroutineScope p1, Continuation<? super Unit> p2) {
                return (this.create(p1, p2)).invokeSuspend(Unit.INSTANCE);
            }
        }), (int)3, null);
    }

    public final void syncSystemPairedDevices() {
        BuildersKt.launch$default((CoroutineScope)ViewModelKt.getViewModelScope((ViewModel)((ViewModel)this)), null, null, (Function2)((Function2)new Function2<CoroutineScope, Continuation<? super Unit>, Object>(this, null){
            Object L$0;
            Object L$1;
            Object L$2;
            Object L$3;
            Object L$4;
            Object L$5;
            Object L$6;
            Object L$7;
            Object L$8;
            int I$0;
            int I$1;
            int label;
            final /* synthetic */ ParkingViewModel this$0;
            {
                this.this$0 = $receiver;
                super(2, $completion);
            }

            /*
             * Unable to fully structure code
             */
            public final Object invokeSuspend(Object $result) {
                var16_2 = IntrinsicsKt.getCOROUTINE_SUSPENDED();
                block0 : switch (this.label) {
                    case 0: {
                        ResultKt.throwOnFailure((Object)$result);
                        systemBonded = BluetoothDeviceHelper.INSTANCE.getSystemBondedDevices((Context)this.this$0.getApplication());
                        var3_4 = systemBonded;
                        if (var3_4 == null || var3_4.isEmpty() != false) break;
                        this.L$0 = systemBonded;
                        this.label = 1;
                        v0 = ParkingViewModel.access$getRepository$p(this.this$0).getAllDevicesDirect((Continuation<? super List<BluetoothCarDevice>>)((Continuation)this));
                        if (v0 == var16_2) {
                            return var16_2;
                        }
                        ** GOTO lbl18
                    }
                    case 1: {
                        systemBonded = (List)this.L$0;
                        ResultKt.throwOnFailure((Object)$result);
                        v0 = $result;
lbl18:
                        // 2 sources

                        existing = (List)v0;
                        $this$associateBy\1 = existing;
                        $i$f$associateBy\1\627 = false;
                        capacity\1 = RangesKt.coerceAtLeast((int)MapsKt.mapCapacity((int)CollectionsKt.collectionSizeOrDefault((Iterable)$this$associateBy\1, (int)10)), (int)16);
                        var8_9 = $this$associateBy\1;
                        destination\2 = new LinkedHashMap<K, V>(capacity\1);
                        $i$f$associateByTo\2\1138 = false;
                        for (T element\2 : $this$associateByTo\2) {
                            var13_16 = (BluetoothCarDevice)element\2;
                            var15_18 = destination\2;
                            $i$a$-associateBy-ParkingViewModel$syncSystemPairedDevices$1$existingMap$1\3\1140\0 = false;
                            v1 = it\3.getAddress().toUpperCase(Locale.ROOT);
                            Intrinsics.checkNotNullExpressionValue((Object)v1, (String)"toUpperCase(...)");
                            var15_18.put(v1, element\2);
                        }
                        existingMap = destination\2;
                        $this$associateBy\1 = systemBonded;
                        var6_7 = this.this$0;
                        $i$f$forEach\4\629 = 0;
                        var8_9 = $this$forEach\4.iterator();
lbl40:
                        // 6 sources

                        while (var8_9.hasNext()) {
                            element\4 = var8_9.next();
                            dev\5 = (BluetoothCarDevice)element\4;
                            $i$a$-forEach-ParkingViewModel$syncSystemPairedDevices$1$1\5\1143\0 = 0;
                            v2 = dev\5.getAddress().toUpperCase(Locale.ROOT);
                            Intrinsics.checkNotNullExpressionValue((Object)v2, (String)"toUpperCase(...)");
                            found\5 = (BluetoothCarDevice)existingMap.get(v2);
                            if (found\5 != null) break block0;
                            this.L$0 = SpillingKt.nullOutSpilledVariable((Object)systemBonded);
                            this.L$1 = SpillingKt.nullOutSpilledVariable((Object)existing);
                            this.L$2 = existingMap;
                            this.L$3 = SpillingKt.nullOutSpilledVariable((Object)$this$forEach\4);
                            this.L$4 = var6_7;
                            this.L$5 = var8_9;
                            this.L$6 = SpillingKt.nullOutSpilledVariable((Object)element\4);
                            this.L$7 = SpillingKt.nullOutSpilledVariable((Object)dev\5);
                            this.L$8 = SpillingKt.nullOutSpilledVariable((Object)found\5);
                            this.I$0 = $i$f$forEach\4\629;
                            this.I$1 = $i$a$-forEach-ParkingViewModel$syncSystemPairedDevices$1$1\5\1143\0;
                            this.label = 2;
                            v3 = ParkingViewModel.access$getRepository$p(var6_7).registerBluetoothDevice(dev\5, false, (Continuation<? super Unit>)this);
                            if (v3 != var16_2) continue;
                            return var16_2;
                        }
                        break;
                    }
                    case 2: {
                        $i$a$-forEach-ParkingViewModel$syncSystemPairedDevices$1$1\5\1143\0 = this.I$1;
                        $i$f$forEach\4\629 = this.I$0;
                        found\5 = (BluetoothCarDevice)this.L$8;
                        dev\5 = (BluetoothCarDevice)this.L$7;
                        element\4 = this.L$6;
                        var8_9 = (Iterator)this.L$5;
                        var6_7 = (ParkingViewModel)this.L$4;
                        $this$forEach\4 = (Iterable)this.L$3;
                        existingMap = (Map)this.L$2;
                        existing = (List)this.L$1;
                        systemBonded = (List)this.L$0;
                        ResultKt.throwOnFailure((Object)$result);
                        v3 = $result;
                        ** GOTO lbl40
                    }
                }
                if (found\5.isCustomRenamed() || Intrinsics.areEqual((Object)found\5.getName(), (Object)dev\5.getName())) ** GOTO lbl40
                this.L$0 = SpillingKt.nullOutSpilledVariable((Object)systemBonded);
                this.L$1 = SpillingKt.nullOutSpilledVariable((Object)existing);
                this.L$2 = existingMap;
                this.L$3 = SpillingKt.nullOutSpilledVariable((Object)$this$forEach\4);
                this.L$4 = var6_7;
                this.L$5 = var8_9;
                this.L$6 = SpillingKt.nullOutSpilledVariable((Object)element\4);
                this.L$7 = SpillingKt.nullOutSpilledVariable((Object)dev\5);
                this.L$8 = SpillingKt.nullOutSpilledVariable((Object)found\5);
                this.I$0 = $i$f$forEach\4\629;
                this.I$1 = $i$a$-forEach-ParkingViewModel$syncSystemPairedDevices$1$1\5\1143\0;
                this.label = 3;
                v4 = ParkingViewModel.access$getRepository$p(var6_7).updateBluetoothDevice(BluetoothCarDevice.copy$default(found\5, null, dev\5.getName(), false, 0L, null, false, 61, null), (Continuation<? super Unit>)this);
                if (v4 != var16_2) ** GOTO lbl40
                return var16_2;
                {
                    case 3: {
                        $i$a$-forEach-ParkingViewModel$syncSystemPairedDevices$1$1\5\1143\0 = this.I$1;
                        $i$f$forEach\4\629 = this.I$0;
                        found\5 = (BluetoothCarDevice)this.L$8;
                        dev\5 = (BluetoothCarDevice)this.L$7;
                        element\4 = this.L$6;
                        var8_9 = (Iterator)this.L$5;
                        var6_7 = (ParkingViewModel)this.L$4;
                        $this$forEach\4 = (Iterable)this.L$3;
                        existingMap = (Map)this.L$2;
                        existing = (List)this.L$1;
                        systemBonded = (List)this.L$0;
                        ResultKt.throwOnFailure((Object)$result);
                        v4 = $result;
                        ** GOTO lbl40
                    }
                }
                return Unit.INSTANCE;
                throw new IllegalStateException("call to 'resume' before 'invoke' with coroutine");
            }

            public final Continuation<Unit> create(Object value, Continuation<?> $completion) {
                return (Continuation)new /* invalid duplicate definition of identical inner class */;
            }

            public final Object invoke(CoroutineScope p1, Continuation<? super Unit> p2) {
                return (this.create(p1, p2)).invokeSuspend(Unit.INSTANCE);
            }
        }), (int)3, null);
    }

    public final void selectPrimaryCarDevice(@NotNull String address) {
        Intrinsics.checkNotNullParameter((Object)address, (String)"address");
        BuildersKt.launch$default((CoroutineScope)ViewModelKt.getViewModelScope((ViewModel)((ViewModel)this)), null, null, (Function2)((Function2)new Function2<CoroutineScope, Continuation<? super Unit>, Object>(this, address, null){
            Object L$0;
            Object L$1;
            int label;
            final /* synthetic */ ParkingViewModel this$0;
            final /* synthetic */ String $address;
            {
                this.this$0 = $receiver;
                this.$address = $address;
                super(2, $completion);
            }

            /*
             * Unable to fully structure code
             */
            public final Object invokeSuspend(Object $result) {
                var11_2 = IntrinsicsKt.getCOROUTINE_SUSPENDED();
                switch (this.label) {
                    case 0: {
                        ResultKt.throwOnFailure((Object)$result);
                        this.label = 1;
                        v0 = ParkingViewModel.access$getRepository$p(this.this$0).selectPrimaryCarDevice(this.$address, (Continuation<? super Unit>)((Continuation)this));
                        if (v0 == var11_2) {
                            return var11_2;
                        }
                        ** GOTO lbl13
                    }
                    case 1: {
                        ResultKt.throwOnFailure((Object)$result);
                        v0 = $result;
lbl13:
                        // 2 sources

                        devices = (List)this.this$0.getAllDevices().getValue();
                        var4_5 = devices;
                        var5_6 = this.$address;
                        var6_7 = var4_5;
                        for (T var8_9 : var6_7) {
                            it\2 = (BluetoothCarDevice)var8_9;
                            $i$a$-find-ParkingViewModel$selectPrimaryCarDevice$1$selected$1\2\646\0 = false;
                            if (!Intrinsics.areEqual((Object)it\2.getAddress(), (Object)var5_6)) continue;
                            v1 = var8_9;
                            ** GOTO lbl24
                        }
                        v1 = null;
lbl24:
                        // 2 sources

                        selected = v1;
                        this.L$0 = SpillingKt.nullOutSpilledVariable((Object)devices);
                        this.L$1 = SpillingKt.nullOutSpilledVariable((Object)selected);
                        this.label = 2;
                        v2 = BuildersKt.withContext((CoroutineContext)((CoroutineContext)Dispatchers.getMain()), (Function2)((Function2)new Function2<CoroutineScope, Continuation<? super Unit>, Object>(this.this$0, selected, null){
                            int label;
                            final /* synthetic */ ParkingViewModel this$0;
                            final /* synthetic */ BluetoothCarDevice $selected;
                            {
                                this.this$0 = $receiver;
                                this.$selected = $selected;
                                super(2, $completion);
                            }

                            public final Object invokeSuspend(Object $result) {
                                IntrinsicsKt.getCOROUTINE_SUSPENDED();
                                switch (this.label) {
                                    case 0: {
                                        ResultKt.throwOnFailure((Object)$result);
                                        Context context = (Context)this.this$0.getApplication();
                                        Object object = this.$selected;
                                        if (object == null || (object = ((BluetoothCarDevice)object).getName()) == null) {
                                            object = "Device";
                                        }
                                        ParkingNotificationHelper.INSTANCE.showSystemNotification(context, "Pixel Parking", "'" + (String)object + "' enabled for Auto-Parking");
                                        return Unit.INSTANCE;
                                    }
                                }
                                throw new IllegalStateException("call to 'resume' before 'invoke' with coroutine");
                            }

                            public final Continuation<Unit> create(Object value, Continuation<?> $completion) {
                                return (Continuation)new /* invalid duplicate definition of identical inner class */;
                            }

                            public final Object invoke(CoroutineScope p1, Continuation<? super Unit> p2) {
                                return (this.create(p1, p2)).invokeSuspend(Unit.INSTANCE);
                            }
                        }), (Continuation)((Continuation)this));
                        if (v2 == var11_2) {
                            return var11_2;
                        }
                        ** GOTO lbl37
                    }
                    case 2: {
                        selected = (BluetoothCarDevice)this.L$1;
                        devices = (List)this.L$0;
                        ResultKt.throwOnFailure((Object)$result);
                        v2 = $result;
lbl37:
                        // 2 sources

                        return Unit.INSTANCE;
                    }
                }
                throw new IllegalStateException("call to 'resume' before 'invoke' with coroutine");
            }

            public final Continuation<Unit> create(Object value, Continuation<?> $completion) {
                return (Continuation)new /* invalid duplicate definition of identical inner class */;
            }

            public final Object invoke(CoroutineScope p1, Continuation<? super Unit> p2) {
                return (this.create(p1, p2)).invokeSuspend(Unit.INSTANCE);
            }
        }), (int)3, null);
    }

    public final void toggleMonitoredDevice(@NotNull String address, boolean isMonitored) {
        Intrinsics.checkNotNullParameter((Object)address, (String)"address");
        BuildersKt.launch$default((CoroutineScope)ViewModelKt.getViewModelScope((ViewModel)((ViewModel)this)), null, null, (Function2)((Function2)new Function2<CoroutineScope, Continuation<? super Unit>, Object>(this, address, isMonitored, null){
            int label;
            final /* synthetic */ ParkingViewModel this$0;
            final /* synthetic */ String $address;
            final /* synthetic */ boolean $isMonitored;
            {
                this.this$0 = $receiver;
                this.$address = $address;
                this.$isMonitored = $isMonitored;
                super(2, $completion);
            }

            /*
             * Enabled force condition propagation
             * Lifted jumps to return sites
             */
            public final Object invokeSuspend(Object $result) {
                Object object = IntrinsicsKt.getCOROUTINE_SUSPENDED();
                switch (this.label) {
                    case 0: {
                        ResultKt.throwOnFailure((Object)$result);
                        this.label = 1;
                        Object object2 = ParkingViewModel.access$getRepository$p(this.this$0).setDeviceMonitored(this.$address, this.$isMonitored, (Continuation<? super Unit>)((Continuation)this));
                        if (object2 != object) return Unit.INSTANCE;
                        return object;
                    }
                    case 1: {
                        ResultKt.throwOnFailure((Object)$result);
                        Object object2 = $result;
                        return Unit.INSTANCE;
                    }
                }
                throw new IllegalStateException("call to 'resume' before 'invoke' with coroutine");
            }

            public final Continuation<Unit> create(Object value, Continuation<?> $completion) {
                return (Continuation)new /* invalid duplicate definition of identical inner class */;
            }

            public final Object invoke(CoroutineScope p1, Continuation<? super Unit> p2) {
                return (this.create(p1, p2)).invokeSuspend(Unit.INSTANCE);
            }
        }), (int)3, null);
    }

    public final void renameBluetoothDevice(@NotNull String address, @NotNull String newName) {
        Intrinsics.checkNotNullParameter((Object)address, (String)"address");
        Intrinsics.checkNotNullParameter((Object)newName, (String)"newName");
        BuildersKt.launch$default((CoroutineScope)ViewModelKt.getViewModelScope((ViewModel)((ViewModel)this)), null, null, (Function2)((Function2)new Function2<CoroutineScope, Continuation<? super Unit>, Object>(this, address, newName, null){
            int label;
            final /* synthetic */ ParkingViewModel this$0;
            final /* synthetic */ String $address;
            final /* synthetic */ String $newName;
            {
                this.this$0 = $receiver;
                this.$address = $address;
                this.$newName = $newName;
                super(2, $completion);
            }

            /*
             * Unable to fully structure code
             */
            public final Object invokeSuspend(Object $result) {
                var2_2 = IntrinsicsKt.getCOROUTINE_SUSPENDED();
                switch (this.label) {
                    case 0: {
                        ResultKt.throwOnFailure((Object)$result);
                        this.label = 1;
                        v0 = ParkingViewModel.access$getRepository$p(this.this$0).renameBluetoothDevice(this.$address, StringsKt.trim((CharSequence)this.$newName).toString(), (Continuation<? super Unit>)((Continuation)this));
                        if (v0 == var2_2) {
                            return var2_2;
                        }
                        ** GOTO lbl13
                    }
                    case 1: {
                        ResultKt.throwOnFailure((Object)$result);
                        v0 = $result;
lbl13:
                        // 2 sources

                        this.label = 2;
                        v1 = BuildersKt.withContext((CoroutineContext)((CoroutineContext)Dispatchers.getMain()), (Function2)((Function2)new Function2<CoroutineScope, Continuation<? super Unit>, Object>(this.this$0, this.$newName, null){
                            int label;
                            final /* synthetic */ ParkingViewModel this$0;
                            final /* synthetic */ String $newName;
                            {
                                this.this$0 = $receiver;
                                this.$newName = $newName;
                                super(2, $completion);
                            }

                            public final Object invokeSuspend(Object $result) {
                                IntrinsicsKt.getCOROUTINE_SUSPENDED();
                                switch (this.label) {
                                    case 0: {
                                        ResultKt.throwOnFailure((Object)$result);
                                        ParkingNotificationHelper.INSTANCE.showSystemNotification((Context)this.this$0.getApplication(), "Pixel Parking", "Renamed vehicle to '" + ((Object)StringsKt.trim((CharSequence)this.$newName)).toString() + "'");
                                        return Unit.INSTANCE;
                                    }
                                }
                                throw new IllegalStateException("call to 'resume' before 'invoke' with coroutine");
                            }

                            public final Continuation<Unit> create(Object value, Continuation<?> $completion) {
                                return (Continuation)new /* invalid duplicate definition of identical inner class */;
                            }

                            public final Object invoke(CoroutineScope p1, Continuation<? super Unit> p2) {
                                return (this.create(p1, p2)).invokeSuspend(Unit.INSTANCE);
                            }
                        }), (Continuation)((Continuation)this));
                        if (v1 == var2_2) {
                            return var2_2;
                        }
                        ** GOTO lbl21
                    }
                    case 2: {
                        ResultKt.throwOnFailure((Object)$result);
                        v1 = $result;
lbl21:
                        // 2 sources

                        return Unit.INSTANCE;
                    }
                }
                throw new IllegalStateException("call to 'resume' before 'invoke' with coroutine");
            }

            public final Continuation<Unit> create(Object value, Continuation<?> $completion) {
                return (Continuation)new /* invalid duplicate definition of identical inner class */;
            }

            public final Object invoke(CoroutineScope p1, Continuation<? super Unit> p2) {
                return (this.create(p1, p2)).invokeSuspend(Unit.INSTANCE);
            }
        }), (int)3, null);
    }

    public final void deleteBluetoothDevice(@NotNull String address) {
        Intrinsics.checkNotNullParameter((Object)address, (String)"address");
        BuildersKt.launch$default((CoroutineScope)ViewModelKt.getViewModelScope((ViewModel)((ViewModel)this)), null, null, (Function2)((Function2)new Function2<CoroutineScope, Continuation<? super Unit>, Object>(this, address, null){
            int label;
            final /* synthetic */ ParkingViewModel this$0;
            final /* synthetic */ String $address;
            {
                this.this$0 = $receiver;
                this.$address = $address;
                super(2, $completion);
            }

            /*
             * Enabled force condition propagation
             * Lifted jumps to return sites
             */
            public final Object invokeSuspend(Object $result) {
                Object object = IntrinsicsKt.getCOROUTINE_SUSPENDED();
                switch (this.label) {
                    case 0: {
                        ResultKt.throwOnFailure((Object)$result);
                        this.label = 1;
                        Object object2 = ParkingViewModel.access$getRepository$p(this.this$0).deleteBluetoothDevice(this.$address, (Continuation<? super Unit>)((Continuation)this));
                        if (object2 != object) return Unit.INSTANCE;
                        return object;
                    }
                    case 1: {
                        ResultKt.throwOnFailure((Object)$result);
                        Object object2 = $result;
                        return Unit.INSTANCE;
                    }
                }
                throw new IllegalStateException("call to 'resume' before 'invoke' with coroutine");
            }

            public final Continuation<Unit> create(Object value, Continuation<?> $completion) {
                return (Continuation)new /* invalid duplicate definition of identical inner class */;
            }

            public final Object invoke(CoroutineScope p1, Continuation<? super Unit> p2) {
                return (this.create(p1, p2)).invokeSuspend(Unit.INSTANCE);
            }
        }), (int)3, null);
    }

    public final void registerNewDevice(@NotNull String name, @NotNull String address, @NotNull String deviceType) {
        Intrinsics.checkNotNullParameter((Object)name, (String)"name");
        Intrinsics.checkNotNullParameter((Object)address, (String)"address");
        Intrinsics.checkNotNullParameter((Object)deviceType, (String)"deviceType");
        BuildersKt.launch$default((CoroutineScope)ViewModelKt.getViewModelScope((ViewModel)((ViewModel)this)), null, null, (Function2)((Function2)new Function2<CoroutineScope, Continuation<? super Unit>, Object>(this, address, name, deviceType, null){
            int label;
            final /* synthetic */ ParkingViewModel this$0;
            final /* synthetic */ String $address;
            final /* synthetic */ String $name;
            final /* synthetic */ String $deviceType;
            {
                this.this$0 = $receiver;
                this.$address = $address;
                this.$name = $name;
                this.$deviceType = $deviceType;
                super(2, $completion);
            }

            /*
             * Unable to fully structure code
             */
            public final Object invokeSuspend(Object $result) {
                var2_2 = IntrinsicsKt.getCOROUTINE_SUSPENDED();
                switch (this.label) {
                    case 0: {
                        ResultKt.throwOnFailure((Object)$result);
                        this.label = 1;
                        v0 = ParkingViewModel.access$getRepository$p(this.this$0).registerBluetoothDevice(new BluetoothCarDevice(this.$address, this.$name, true, 0L, this.$deviceType, false, 40, null), false, (Continuation<? super Unit>)((Continuation)this));
                        if (v0 == var2_2) {
                            return var2_2;
                        }
                        ** GOTO lbl13
                    }
                    case 1: {
                        ResultKt.throwOnFailure((Object)$result);
                        v0 = $result;
lbl13:
                        // 2 sources

                        this.label = 2;
                        v1 = BuildersKt.withContext((CoroutineContext)((CoroutineContext)Dispatchers.getMain()), (Function2)((Function2)new Function2<CoroutineScope, Continuation<? super Unit>, Object>(this.this$0, this.$name, null){
                            int label;
                            final /* synthetic */ ParkingViewModel this$0;
                            final /* synthetic */ String $name;
                            {
                                this.this$0 = $receiver;
                                this.$name = $name;
                                super(2, $completion);
                            }

                            public final Object invokeSuspend(Object $result) {
                                IntrinsicsKt.getCOROUTINE_SUSPENDED();
                                switch (this.label) {
                                    case 0: {
                                        ResultKt.throwOnFailure((Object)$result);
                                        ParkingNotificationHelper.INSTANCE.showSystemNotification((Context)this.this$0.getApplication(), "Pixel Parking", "'" + this.$name + "' added & enabled for Auto-Park");
                                        return Unit.INSTANCE;
                                    }
                                }
                                throw new IllegalStateException("call to 'resume' before 'invoke' with coroutine");
                            }

                            public final Continuation<Unit> create(Object value, Continuation<?> $completion) {
                                return (Continuation)new /* invalid duplicate definition of identical inner class */;
                            }

                            public final Object invoke(CoroutineScope p1, Continuation<? super Unit> p2) {
                                return (this.create(p1, p2)).invokeSuspend(Unit.INSTANCE);
                            }
                        }), (Continuation)((Continuation)this));
                        if (v1 == var2_2) {
                            return var2_2;
                        }
                        ** GOTO lbl21
                    }
                    case 2: {
                        ResultKt.throwOnFailure((Object)$result);
                        v1 = $result;
lbl21:
                        // 2 sources

                        return Unit.INSTANCE;
                    }
                }
                throw new IllegalStateException("call to 'resume' before 'invoke' with coroutine");
            }

            public final Continuation<Unit> create(Object value, Continuation<?> $completion) {
                return (Continuation)new /* invalid duplicate definition of identical inner class */;
            }

            public final Object invoke(CoroutineScope p1, Continuation<? super Unit> p2) {
                return (this.create(p1, p2)).invokeSuspend(Unit.INSTANCE);
            }
        }), (int)3, null);
    }

    public final void openSystemBluetoothSettings(@NotNull Context context) {
        Intrinsics.checkNotNullParameter((Object)context, (String)"context");
        try {
            Intent intent;
            Intent intent2 = intent = new Intent("android.settings.BLUETOOTH_SETTINGS");
            boolean bl = false;
            intent2.setFlags(0x10000000);
            Intent intent3 = intent;
            context.startActivity(intent3);
        }
        catch (Exception e) {
            ParkingNotificationHelper.INSTANCE.showSystemNotification((Context)this.getApplication(), "Pixel Parking", "Please open Bluetooth Settings from system settings");
        }
    }

    public final void simulateBluetoothDisconnect(@NotNull String deviceName, @NotNull String deviceAddress) {
        Intent intent;
        Intrinsics.checkNotNullParameter((Object)deviceName, (String)"deviceName");
        Intrinsics.checkNotNullParameter((Object)deviceAddress, (String)"deviceAddress");
        Intent intent2 = intent = new Intent("com.example.autopark.ACTION_SIMULATE_DISCONNECT");
        boolean bl = false;
        intent2.setPackage(this.getApplication().getPackageName());
        intent2.putExtra("simulated_name", deviceName);
        intent2.putExtra("simulated_address", deviceAddress);
        Intent intent3 = intent;
        this.getApplication().sendBroadcast(intent3);
        ParkingNotificationHelper.INSTANCE.showSystemNotification((Context)this.getApplication(), "Pixel Parking", "Simulating BT disconnect for '" + deviceName + "'...");
    }

    public final void openGoogleMapsNavigation(@NotNull Context context, @Nullable ParkingSpot spot) {
        Intent intent;
        CharSequence charSequence;
        Intrinsics.checkNotNullParameter((Object)context, (String)"context");
        if (spot == null) {
            ParkingNotificationHelper.INSTANCE.showSystemNotification((Context)this.getApplication(), "Pixel Parking", "No active parking spot to navigate to");
            return;
        }
        double lat = spot.getLatitude();
        double lng = spot.getLongitude();
        CharSequence charSequence2 = spot.getSpotName();
        if (StringsKt.isBlank((CharSequence)charSequence2)) {
            boolean bl = false;
            charSequence = "Parked Car";
        } else {
            charSequence = charSequence2;
        }
        String label = Uri.encode((String)((String)charSequence));
        Uri mapsAppUri = Uri.parse((String)("google.navigation:q=" + lat + "," + lng + "&mode=w"));
        Intent intent2 = intent = new Intent("android.intent.action.VIEW", mapsAppUri);
        boolean bl = false;
        intent2.setPackage("com.google.android.apps.maps");
        intent2.setFlags(0x10000000);
        Intent mapsAppIntent = intent;
        Uri geoUri = Uri.parse((String)("geo:" + lat + "," + lng + "?q=" + lat + "," + lng + "(" + label + ")"));
        Intent intent3 = bl = new Intent("android.intent.action.VIEW", geoUri);
        boolean bl2 = false;
        intent3.setFlags(0x10000000);
        Intent geoIntent = bl;
        Uri browserUri = Uri.parse((String)("https://www.google.com/maps/dir/?api=1&destination=" + lat + "," + lng + "&travelmode=walking"));
        Intent intent4 = bl2 = new Intent("android.intent.action.VIEW", browserUri);
        boolean bl3 = false;
        intent4.setFlags(0x10000000);
        Intent browserIntent = bl2;
        try {
            context.startActivity(mapsAppIntent);
        }
        catch (Exception e1) {
            try {
                context.startActivity(geoIntent);
            }
            catch (Exception e2) {
                try {
                    context.startActivity(browserIntent);
                }
                catch (Exception e3) {
                    ParkingNotificationHelper.INSTANCE.showSystemNotification((Context)this.getApplication(), "Pixel Parking", "Could not open Maps navigation");
                }
            }
        }
    }

    public static /* synthetic */ void openGoogleMapsNavigation$default(ParkingViewModel parkingViewModel, Context context, ParkingSpot parkingSpot, int n, Object object) {
        if ((n & 2) != 0) {
            parkingSpot = (ParkingSpot)parkingViewModel.activeSpot.getValue();
        }
        parkingViewModel.openGoogleMapsNavigation(context, parkingSpot);
    }

    public final void shareParkingLocation(@NotNull Context context, @Nullable ParkingSpot spot) {
        ParkingSpot targetSpot;
        Intrinsics.checkNotNullParameter((Object)context, (String)"context");
        ParkingSpot parkingSpot = spot;
        if (parkingSpot == null && (parkingSpot = (ParkingSpot)this.activeSpot.getValue()) == null) {
            parkingSpot = (ParkingSpot)CollectionsKt.firstOrNull((List)((List)this.allSpots.getValue()));
        }
        if ((targetSpot = parkingSpot) != null) {
            StringBuilder stringBuilder;
            StringBuilder stringBuilder2 = stringBuilder = new StringBuilder();
            boolean bl = false;
            stringBuilder2.append("\ud83d\udccd " + targetSpot.getSpotName() + "\n");
            if (!StringsKt.isBlank((CharSequence)targetSpot.getAddress())) {
                stringBuilder2.append(targetSpot.getAddress() + "\n");
            }
            if (!StringsKt.isBlank((CharSequence)targetSpot.getFloorLevel())) {
                stringBuilder2.append("Floor/Level: " + targetSpot.getFloorLevel() + "\n");
            }
            if (!StringsKt.isBlank((CharSequence)targetSpot.getNote())) {
                stringBuilder2.append("Notes: " + targetSpot.getNote() + "\n");
            }
            stringBuilder2.append("https://maps.google.com/?q=" + targetSpot.getLatitude() + "," + targetSpot.getLongitude());
            String shareText = stringBuilder.toString();
            StringBuilder stringBuilder3 = stringBuilder2 = new Intent("android.intent.action.SEND");
            boolean bl2 = false;
            stringBuilder3.setType("text/plain");
            stringBuilder3.putExtra("android.intent.extra.SUBJECT", targetSpot.getSpotName());
            stringBuilder3.putExtra("android.intent.extra.TEXT", shareText);
            StringBuilder sendIntent = stringBuilder2;
            StringBuilder stringBuilder4 = stringBuilder3 = Intent.createChooser((Intent)sendIntent, (CharSequence)"Share Parking Location");
            boolean bl3 = false;
            stringBuilder4.addFlags(0x10000000);
            StringBuilder chooserIntent = stringBuilder3;
            try {
                context.startActivity((Intent)chooserIntent);
            }
            catch (Exception e) {
                Toast.makeText((Context)context, (CharSequence)"Could not open share menu", (int)0).show();
            }
            return;
        }
        Location loc = (Location)this.currentLocation.getValue();
        if (loc != null) {
            Intent e;
            String shareText = "\ud83d\udccd My Current Location\n" + (String)(!StringsKt.isBlank((CharSequence)((CharSequence)this.currentAddress.getValue())) ? this.currentAddress.getValue() + "\n" : "") + "https://maps.google.com/?q=" + loc.getLatitude() + "," + loc.getLongitude();
            Intent intent = e = new Intent("android.intent.action.SEND");
            boolean bl = false;
            intent.setType("text/plain");
            intent.putExtra("android.intent.extra.SUBJECT", "My Location");
            intent.putExtra("android.intent.extra.TEXT", shareText);
            Intent sendIntent = e;
            Intent intent2 = intent = Intent.createChooser((Intent)sendIntent, (CharSequence)"Share Location");
            boolean bl4 = false;
            intent2.addFlags(0x10000000);
            Intent chooserIntent = intent;
            try {
                context.startActivity(chooserIntent);
            }
            catch (Exception e2) {
                Toast.makeText((Context)context, (CharSequence)"Could not open share menu", (int)0).show();
            }
        } else {
            Toast.makeText((Context)context, (CharSequence)"No parking spot or GPS location to share", (int)0).show();
        }
    }

    public static /* synthetic */ void shareParkingLocation$default(ParkingViewModel parkingViewModel, Context context, ParkingSpot parkingSpot, int n, Object object) {
        if ((n & 2) != 0) {
            parkingSpot = null;
        }
        parkingViewModel.shareParkingLocation(context, parkingSpot);
    }

    public final void toggleRadarService() {
        ParkingSpot parkingSpot = (ParkingSpot)this.activeSpot.getValue();
        if (parkingSpot == null) {
            return;
        }
        ParkingSpot current = parkingSpot;
        if (((Boolean)this._isRadarServiceRunning.getValue()).booleanValue()) {
            ParkingRadarService.Companion.stop((Context)this.getApplication());
            this._isRadarServiceRunning.setValue((Object)false);
        } else {
            String dist = ((NavigationTelemetry)this.navigationTelemetry.getValue()).getFormattedDistance();
            ParkingRadarService.Companion.start((Context)this.getApplication(), current.getSpotName(), dist + " away \u2022 Level: " + current.getFloorLevel());
            this._isRadarServiceRunning.setValue((Object)true);
        }
    }

    public final void stopRadarService() {
        if (((Boolean)this._isRadarServiceRunning.getValue()).booleanValue()) {
            ParkingRadarService.Companion.stop((Context)this.getApplication());
            this._isRadarServiceRunning.setValue((Object)false);
        }
    }

    public final void unlockDeveloperMode() {
        this._isDeveloperUnlocked.setValue((Object)true);
        this.prefs.edit().putBoolean("dev_mode_unlocked", true).apply();
    }

    public final void setDevMockGpsEnabled(boolean enabled) {
        this._isDevMockGpsEnabled.setValue((Object)enabled);
        this.prefs.edit().putBoolean("dev_mock_gps", enabled).apply();
    }

    public final void setBtProximityEnabled(boolean enabled) {
        this._isBtProximityEnabled.setValue((Object)enabled);
        this.prefs.edit().putBoolean("bt_proximity_enabled", enabled).apply();
        if (!enabled) {
            this.stopBtProximityFinder();
        }
    }

    public final void setDevHapticDiagnostics(boolean enabled) {
        this._isDevHapticDiagnostics.setValue((Object)enabled);
        this.prefs.edit().putBoolean("dev_haptic_diag", enabled).apply();
    }

    public final void setParkingTimerFeatureEnabled(boolean enabled) {
        this._isParkingTimerFeatureEnabled.setValue((Object)enabled);
        this.prefs.edit().putBoolean("parking_timer_feature_enabled", enabled).apply();
        if (!enabled) {
            this.resetTimerToZero();
        }
    }

    public final boolean startBtProximityFinder(@NotNull BluetoothCarDevice device) {
        Intrinsics.checkNotNullParameter((Object)device, (String)"device");
        boolean isConnected = BluetoothDeviceHelper.INSTANCE.isDeviceConnected((Context)this.getApplication(), device.getAddress());
        if (!isConnected) {
            return false;
        }
        this._activeBtProximityDevice.setValue((Object)device);
        Job job = this.btProximityJob;
        if (job != null) {
            Job.DefaultImpls.cancel$default((Job)job, null, (int)1, null);
        }
        this.btProximityJob = BuildersKt.launch$default((CoroutineScope)ViewModelKt.getViewModelScope((ViewModel)((ViewModel)this)), (CoroutineContext)((CoroutineContext)Dispatchers.getDefault()), null, (Function2)((Function2)new Function2<CoroutineScope, Continuation<? super Unit>, Object>(this, null){
            Object L$1;
            Object L$2;
            int I$0;
            int I$1;
            int I$2;
            float F$0;
            float F$1;
            float F$2;
            float F$3;
            int label;
            private /* synthetic */ Object L$0;
            final /* synthetic */ ParkingViewModel this$0;
            {
                this.this$0 = $receiver;
                super(2, $completion);
            }

            /*
             * Unable to fully structure code
             */
            public final Object invokeSuspend(Object $result) {
                var2_2 = (CoroutineScope)this.L$0;
                var22_3 = IntrinsicsKt.getCOROUTINE_SUSPENDED();
                switch (this.label) {
                    case 0: {
                        ResultKt.throwOnFailure((Object)$result);
                        rssiHistory = new ArrayList<E>();
                        currentRssi = -60;
                        targetRssi = -52;
                        estimatedTargetBearing = (((CompassState)ParkingViewModel.access$getCompassSensorManager$p(this.this$0).getCompassState().getValue()).getAzimuthDegrees() + 45.0f) % 360.0f;
lbl10:
                        // 3 sources

                        while (CoroutineScopeKt.isActive((CoroutineScope)$this$launch) && ParkingViewModel.access$get_activeBtProximityDevice$p(this.this$0).getValue() != null) {
                            currentAzimuth = ((CompassState)ParkingViewModel.access$getCompassSensorManager$p(this.this$0).getCompassState().getValue()).getAzimuthDegrees();
                            if (Random.Default.nextFloat() < 0.25f) {
                                targetRssi = RangesKt.coerceIn((int)(targetRssi + Random.Default.nextInt(-5, 6)), (int)-88, (int)-35);
                            }
                            if (currentRssi < targetRssi) {
                                ++currentRssi;
                            } else if (currentRssi > targetRssi) {
                                --currentRssi;
                            }
                            ParkingViewModel.access$get_btProximityRssi$p(this.this$0).setValue((Object)Boxing.boxInt((int)currentRssi));
                            clamped = RangesKt.coerceIn((int)currentRssi, (int)-88, (int)-35);
                            distance = RangesKt.coerceIn((float)((float)Math.pow(10.0, (-48.0 - (double)clamped) / 20.0)), (float)1.2f, (float)38.0f);
                            ParkingViewModel.access$get_btProximityDistanceMeters$p(this.this$0).setValue((Object)Boxing.boxFloat((float)distance));
                            rssiHistory.add(new Pair((Object)Boxing.boxFloat((float)currentAzimuth), (Object)Boxing.boxInt((int)currentRssi)));
                            if (rssiHistory.size() > 20) {
                                rssiHistory.remove(0);
                            }
                            $this$sortedByDescending\1 = rssiHistory;
                            $i$f$sortedByDescending\1\898 = false;
                            bestSamples = CollectionsKt.take((Iterable)CollectionsKt.sortedWith((Iterable)$this$sortedByDescending\1, (Comparator)new Comparator(){

                                /*
                                 * WARNING - void declaration
                                 */
                                public final int compare(T a, T b) {
                                    void it\2;
                                    Pair pair = (Pair)b;
                                    boolean bl = false;
                                    Comparable comparable = (Integer)pair.getSecond();
                                    pair = (Pair)a;
                                    Comparable comparable2 = comparable;
                                    boolean bl2 = false;
                                    return ComparisonsKt.compareValues((Comparable)comparable2, (Comparable)((Integer)it\2.getSecond()));
                                }
                            }), (int)4);
                            if (((Collection)bestSamples).isEmpty() == false) {
                                $this$map\2 = bestSamples;
                                $i$f$map\2\900 = false;
                                var14_18 = $this$map\2;
                                destination\3 = new ArrayList<E>(CollectionsKt.collectionSizeOrDefault((Iterable)$this$map\2, (int)10));
                                $i$f$mapTo\3\1138 = false;
                                for (T item\3 : $this$mapTo\3) {
                                    var19_23 = (Pair)item\3;
                                    var21_25 = destination\3;
                                    $i$a$-map-ParkingViewModel$startBtProximityFinder$1$peakAzimuth$1\4\1140\0 = false;
                                    var21_25.add(Boxing.boxFloat((float)((Number)it\4.getFirst()).floatValue()));
                                }
                                estimatedTargetBearing = peakAzimuth = (float)CollectionsKt.averageOfFloat((Iterable)((List)destination\3));
                            }
                            relativeAngle = (estimatedTargetBearing - currentAzimuth + 360.0f) % 360.0f;
                            ParkingViewModel.access$get_btProximityRelativeAngle$p(this.this$0).setValue((Object)Boxing.boxFloat((float)relativeAngle));
                            this.L$0 = $this$launch;
                            this.L$1 = rssiHistory;
                            this.L$2 = SpillingKt.nullOutSpilledVariable((Object)bestSamples);
                            this.I$0 = currentRssi;
                            this.I$1 = targetRssi;
                            this.F$0 = estimatedTargetBearing;
                            this.F$1 = currentAzimuth;
                            this.I$2 = clamped;
                            this.F$2 = distance;
                            this.F$3 = relativeAngle;
                            this.label = 1;
                            v0 = DelayKt.delay((long)180L, (Continuation)((Continuation)this));
                            if (v0 != var22_3) continue;
                            return var22_3;
                        }
                        break;
                    }
                    case 1: {
                        relativeAngle = this.F$3;
                        distance = this.F$2;
                        clamped = this.I$2;
                        currentAzimuth = this.F$1;
                        estimatedTargetBearing = this.F$0;
                        targetRssi = this.I$1;
                        currentRssi = this.I$0;
                        bestSamples = (List)this.L$2;
                        rssiHistory = (List)this.L$1;
                        ResultKt.throwOnFailure((Object)$result);
                        v0 = $result;
                        ** GOTO lbl10
                    }
                }
                return Unit.INSTANCE;
                throw new IllegalStateException("call to 'resume' before 'invoke' with coroutine");
            }

            public final Continuation<Unit> create(Object value, Continuation<?> $completion) {
                var var3_3 = new /* invalid duplicate definition of identical inner class */;
                var3_3.L$0 = value;
                return (Continuation)var3_3;
            }

            public final Object invoke(CoroutineScope p1, Continuation<? super Unit> p2) {
                return (this.create(p1, p2)).invokeSuspend(Unit.INSTANCE);
            }
        }), (int)2, null);
        return true;
    }

    public final void stopBtProximityFinder() {
        Job job = this.btProximityJob;
        if (job != null) {
            Job.DefaultImpls.cancel$default((Job)job, null, (int)1, null);
        }
        this.btProximityJob = null;
        this._activeBtProximityDevice.setValue(null);
    }

    public final void openParkingTimer() {
        this._showTimerDialog.setValue((Object)true);
    }

    public final void closeParkingTimer() {
        this._showTimerDialog.setValue((Object)false);
    }

    public final void startParkingTimer() {
        if (((Number)this._timerRemainingSeconds.getValue()).longValue() <= 0L) {
            this._timerRemainingSeconds.setValue(this._timerTotalSeconds.getValue());
        }
        this._timerIsRunning.setValue((Object)true);
        this._timerIsPaused.setValue((Object)false);
        this._isAlarmActive.setValue((Object)false);
        AlarmSoundHelper.INSTANCE.stopAlarm((Context)this.getApplication());
        ParkingSpot parkingSpot = (ParkingSpot)this.activeSpot.getValue();
        if (parkingSpot != null) {
            ParkingSpot parkingSpot2 = parkingSpot;
            boolean bl = false;
            long l = System.currentTimeMillis() + ((Number)this._timerRemainingSeconds.getValue()).longValue() * 1000L;
            BuildersKt.launch$default((CoroutineScope)ViewModelKt.getViewModelScope((ViewModel)((ViewModel)this)), null, null, (Function2)((Function2)new Function2<CoroutineScope, Continuation<? super Unit>, Object>(this, parkingSpot2, l, null){
                int label;
                final /* synthetic */ ParkingViewModel this$0;
                final /* synthetic */ ParkingSpot $spot;
                final /* synthetic */ long $expiryMs;
                {
                    this.this$0 = $receiver;
                    this.$spot = $spot;
                    this.$expiryMs = $expiryMs;
                    super(2, $completion);
                }

                /*
                 * Enabled force condition propagation
                 * Lifted jumps to return sites
                 */
                public final Object invokeSuspend(Object $result) {
                    Object object = IntrinsicsKt.getCOROUTINE_SUSPENDED();
                    switch (this.label) {
                        case 0: {
                            ResultKt.throwOnFailure((Object)$result);
                            this.label = 1;
                            Object object2 = ParkingViewModel.access$getRepository$p(this.this$0).updateParkingSpot(ParkingSpot.copy$default(this.$spot, 0L, 0.0, 0.0, 0.0, 0.0f, null, null, null, null, null, 0L, false, null, null, Boxing.boxLong((long)this.$expiryMs), false, 49151, null), (Continuation<? super Unit>)((Continuation)this));
                            if (object2 != object) return Unit.INSTANCE;
                            return object;
                        }
                        case 1: {
                            ResultKt.throwOnFailure((Object)$result);
                            Object object2 = $result;
                            return Unit.INSTANCE;
                        }
                    }
                    throw new IllegalStateException("call to 'resume' before 'invoke' with coroutine");
                }

                public final Continuation<Unit> create(Object value, Continuation<?> $completion) {
                    return (Continuation)new /* invalid duplicate definition of identical inner class */;
                }

                public final Object invoke(CoroutineScope p1, Continuation<? super Unit> p2) {
                    return (this.create(p1, p2)).invokeSuspend(Unit.INSTANCE);
                }
            }), (int)3, null);
        }
        Job job = this.timerJob;
        if (job != null) {
            Job.DefaultImpls.cancel$default((Job)job, null, (int)1, null);
        }
        this.timerJob = BuildersKt.launch$default((CoroutineScope)ViewModelKt.getViewModelScope((ViewModel)((ViewModel)this)), null, null, (Function2)((Function2)new Function2<CoroutineScope, Continuation<? super Unit>, Object>(this, null){
            int label;
            final /* synthetic */ ParkingViewModel this$0;
            {
                this.this$0 = $receiver;
                super(2, $completion);
            }

            /*
             * Unable to fully structure code
             */
            public final Object invokeSuspend(Object $result) {
                var12_2 = IntrinsicsKt.getCOROUTINE_SUSPENDED();
                switch (this.label) {
                    case 0: {
                        ResultKt.throwOnFailure((Object)$result);
lbl5:
                        // 2 sources

                        while (((Boolean)ParkingViewModel.access$get_timerIsRunning$p(this.this$0).getValue()).booleanValue() && ((Number)ParkingViewModel.access$get_timerRemainingSeconds$p(this.this$0).getValue()).longValue() > 0L) {
                            this.label = 1;
                            v0 = DelayKt.delay((long)1000L, (Continuation)((Continuation)this));
                            if (v0 == var12_2) {
                                return var12_2;
                            }
                            ** GOTO lbl15
                        }
                        ** GOTO lbl43
                    }
                    case 1: {
                        ResultKt.throwOnFailure((Object)$result);
                        v0 = $result;
lbl15:
                        // 2 sources

                        remaining = ((Number)ParkingViewModel.access$get_timerRemainingSeconds$p(this.this$0).getValue()).longValue() - 1L;
                        ParkingViewModel.access$get_timerRemainingSeconds$p(this.this$0).setValue((Object)Boxing.boxLong((long)remaining));
                        remainingMinutes = (int)(remaining / (long)60);
                        triggered = (Set)ParkingViewModel.access$get_timerAlertsTriggered$p(this.this$0).getValue();
                        reminders = (List)ParkingViewModel.access$get_timerRemindersMinutes$p(this.this$0).getValue();
                        var7_7 = reminders.iterator();
                        while (var7_7.hasNext()) {
                            alertMin = ((Number)var7_7.next()).intValue();
                            if (remainingMinutes > alertMin || remainingMinutes <= 0 || triggered.contains(Boxing.boxInt((int)alertMin))) continue;
                            ParkingViewModel.access$get_timerAlertsTriggered$p(this.this$0).setValue((Object)SetsKt.plus((Set)triggered, (Object)Boxing.boxInt((int)alertMin)));
                            ParkingNotificationHelper.INSTANCE.showSystemNotification((Context)this.this$0.getApplication(), "Parking & Charging Timer", "\u26a0\ufe0f " + alertMin + " minutes remaining before your parking / charging time ends!");
                            if (HapticHelper.INSTANCE.getVibrator((Context)this.this$0.getApplication()) == null) continue;
                            $i$a$-let-ParkingViewModel$startParkingTimer$2$1\1\966\0 = false;
                            if (Build.VERSION.SDK_INT >= 26) {
                                var11_11 = new long[]{0L, 300L, 200L, 300L};
                                v\1.vibrate(VibrationEffect.createWaveform((long[])var11_11, (int)-1));
                                continue;
                            }
                            var11_11 = new long[]{0L, 300L, 200L, 300L};
                            v\1.vibrate(var11_11, -1);
                        }
                        if (remaining > 0L) ** GOTO lbl5
                        ParkingViewModel.access$get_timerIsRunning$p(this.this$0).setValue((Object)Boxing.boxBoolean((boolean)false));
                        ParkingViewModel.access$get_timerIsPaused$p(this.this$0).setValue((Object)Boxing.boxBoolean((boolean)false));
                        ParkingViewModel.access$get_isAlarmActive$p(this.this$0).setValue((Object)Boxing.boxBoolean((boolean)true));
                        ParkingViewModel.access$get_timerRemainingSeconds$p(this.this$0).setValue((Object)Boxing.boxLong((long)0L));
                        AlarmSoundHelper.INSTANCE.playAlarm((Context)this.this$0.getApplication());
                        ParkingNotificationHelper.INSTANCE.showSystemNotification((Context)this.this$0.getApplication(), "\ud83d\udea8 PARKING TIMER EXPIRED!", "Time is up! Your parking or charging session has ended. Move your car or unplug now.");
lbl43:
                        // 2 sources

                        return Unit.INSTANCE;
                    }
                }
                throw new IllegalStateException("call to 'resume' before 'invoke' with coroutine");
            }

            public final Continuation<Unit> create(Object value, Continuation<?> $completion) {
                return (Continuation)new /* invalid duplicate definition of identical inner class */;
            }

            public final Object invoke(CoroutineScope p1, Continuation<? super Unit> p2) {
                return (this.create(p1, p2)).invokeSuspend(Unit.INSTANCE);
            }
        }), (int)3, null);
    }

    public final void pauseParkingTimer() {
        block0: {
            this._timerIsRunning.setValue((Object)false);
            this._timerIsPaused.setValue((Object)true);
            Job job = this.timerJob;
            if (job == null) break block0;
            Job.DefaultImpls.cancel$default((Job)job, null, (int)1, null);
        }
    }

    public final void resetParkingTimer() {
        this._timerIsRunning.setValue((Object)false);
        this._timerIsPaused.setValue((Object)false);
        Job job = this.timerJob;
        if (job != null) {
            Job.DefaultImpls.cancel$default((Job)job, null, (int)1, null);
        }
        this._timerRemainingSeconds.setValue(this._timerTotalSeconds.getValue());
        this._timerAlertsTriggered.setValue((Object)SetsKt.emptySet());
        this._isAlarmActive.setValue((Object)false);
        AlarmSoundHelper.INSTANCE.stopAlarm((Context)this.getApplication());
    }

    public final void resetTimerToZero() {
        this._timerIsRunning.setValue((Object)false);
        this._timerIsPaused.setValue((Object)false);
        Job job = this.timerJob;
        if (job != null) {
            Job.DefaultImpls.cancel$default((Job)job, null, (int)1, null);
        }
        this._timerTotalSeconds.setValue((Object)0L);
        this._timerRemainingSeconds.setValue((Object)0L);
        this.prefs.edit().putLong("parking_timer_total_seconds", 0L).apply();
        this._timerAlertsTriggered.setValue((Object)SetsKt.emptySet());
        this._isAlarmActive.setValue((Object)false);
        AlarmSoundHelper.INSTANCE.stopAlarm((Context)this.getApplication());
    }

    public final void toggleTimerPlayPause() {
        if (((Boolean)this._timerIsRunning.getValue()).booleanValue()) {
            this.pauseParkingTimer();
        } else if (((Number)this._timerRemainingSeconds.getValue()).longValue() > 0L) {
            this.startParkingTimer();
        }
    }

    public final void addTimerMinutes(int minutes) {
        long newTotal = RangesKt.coerceIn((long)(((Number)this._timerTotalSeconds.getValue()).longValue() + (long)minutes * 60L), (long)60L, (long)86400L);
        this._timerTotalSeconds.setValue((Object)newTotal);
        this.prefs.edit().putLong("parking_timer_total_seconds", newTotal).apply();
        long newRemaining = RangesKt.coerceIn((long)(((Number)this._timerRemainingSeconds.getValue()).longValue() + (long)minutes * 60L), (long)0L, (long)86400L);
        this._timerRemainingSeconds.setValue((Object)newRemaining);
    }

    public final void setTimerDuration(long seconds) {
        long clamped = RangesKt.coerceIn((long)seconds, (long)0L, (long)86400L);
        this._timerTotalSeconds.setValue((Object)clamped);
        this.prefs.edit().putLong("parking_timer_total_seconds", clamped).apply();
        if (!((Boolean)this._timerIsRunning.getValue()).booleanValue()) {
            this._timerRemainingSeconds.setValue((Object)clamped);
            this._timerAlertsTriggered.setValue((Object)SetsKt.emptySet());
        }
    }

    public final void adjustTimerByDrag(boolean isClockwise) {
        if (isClockwise) {
            long current = ((Number)this._timerTotalSeconds.getValue()).longValue();
            long newTotal = RangesKt.coerceIn((long)(current + 60L), (long)60L, (long)86400L);
            this._timerTotalSeconds.setValue((Object)newTotal);
            this.prefs.edit().putLong("parking_timer_total_seconds", newTotal).apply();
            if (!((Boolean)this._timerIsRunning.getValue()).booleanValue()) {
                this._timerRemainingSeconds.setValue((Object)newTotal);
            } else {
                this._timerRemainingSeconds.setValue((Object)RangesKt.coerceIn((long)(((Number)this._timerRemainingSeconds.getValue()).longValue() + 60L), (long)1L, (long)86400L));
            }
        } else {
            long current = ((Number)this._timerTotalSeconds.getValue()).longValue();
            long newTotal = RangesKt.coerceAtLeast((long)(current - 60L), (long)0L);
            this._timerTotalSeconds.setValue((Object)newTotal);
            this.prefs.edit().putLong("parking_timer_total_seconds", newTotal).apply();
            if (!((Boolean)this._timerIsRunning.getValue()).booleanValue()) {
                this._timerRemainingSeconds.setValue((Object)newTotal);
            } else {
                this._timerRemainingSeconds.setValue((Object)RangesKt.coerceAtLeast((long)(((Number)this._timerRemainingSeconds.getValue()).longValue() - 60L), (long)0L));
            }
        }
    }

    public final void adjustReminderMinutes(int index, boolean isAdd) {
        List currentList = CollectionsKt.toMutableList((Collection)((Collection)this._timerRemindersMinutes.getValue()));
        while (currentList.size() < 3) {
            int n;
            switch (currentList.size()) {
                case 0: {
                    n = 15;
                    break;
                }
                case 1: {
                    n = 10;
                    break;
                }
                default: {
                    n = 5;
                }
            }
            currentList.add(n);
        }
        boolean bl = 0 <= index ? index < currentList.size() : false;
        if (bl) {
            int currentVal = ((Number)currentList.get(index)).intValue();
            int newVal = isAdd ? RangesKt.coerceIn((int)(currentVal + 1), (int)1, (int)180) : RangesKt.coerceAtLeast((int)(currentVal - 1), (int)0);
            currentList.set(index, newVal);
            this._timerRemindersMinutes.setValue((Object)currentList);
            this.prefs.edit().putString("parking_timer_reminders", CollectionsKt.joinToString$default((Iterable)currentList, (CharSequence)",", null, null, (int)0, null, null, (int)62, null)).apply();
        }
    }

    /*
     * WARNING - void declaration
     */
    public final void setTimerReminders(@NotNull List<Integer> reminders) {
        void $this$filterTo\2;
        Intrinsics.checkNotNullParameter(reminders, (String)"reminders");
        Iterable iterable = reminders;
        boolean bl = false;
        Iterable iterable2 = iterable;
        Collection collection = new ArrayList();
        boolean bl2 = false;
        for (Object t : $this$filterTo\2) {
            int n = ((Number)t).intValue();
            boolean bl3 = false;
            if (!(n > 0)) continue;
            collection.add(t);
        }
        List valid = CollectionsKt.sortedDescending((Iterable)CollectionsKt.distinct((Iterable)CollectionsKt.take((Iterable)((List)collection), (int)3)));
        this._timerRemindersMinutes.setValue((Object)valid);
        this.prefs.edit().putString("parking_timer_reminders", CollectionsKt.joinToString$default((Iterable)valid, (CharSequence)",", null, null, (int)0, null, null, (int)62, null)).apply();
    }

    public final void dismissTimerAlarm() {
        this._isAlarmActive.setValue((Object)false);
        AlarmSoundHelper.INSTANCE.stopAlarm((Context)this.getApplication());
    }

    public final void snoozeTimerAlarm(int minutes) {
        this.dismissTimerAlarm();
        this.setTimerDuration((long)minutes * 60L);
        this.startParkingTimer();
    }

    public static /* synthetic */ void snoozeTimerAlarm$default(ParkingViewModel parkingViewModel, int n, int n2, Object object) {
        if ((n2 & 1) != 0) {
            n = 5;
        }
        parkingViewModel.snoozeTimerAlarm(n);
    }

    public final void updateAlarmSoundUri(@NotNull Uri uri) {
        Intrinsics.checkNotNullParameter((Object)uri, (String)"uri");
        AlarmSoundHelper.INSTANCE.saveAlarmUri((Context)this.getApplication(), uri);
        this._alarmSoundTitle.setValue((Object)AlarmSoundHelper.INSTANCE.getAlarmTitle((Context)this.getApplication(), uri));
    }

    public final void refreshAlarmTitle() {
        this._alarmSoundTitle.setValue((Object)AlarmSoundHelper.getAlarmTitle$default(AlarmSoundHelper.INSTANCE, (Context)this.getApplication(), null, 2, null));
    }

    protected void onCleared() {
        super.onCleared();
        this.stopLocationTracking();
        this.compassSensorManager.stopListening();
    }

    private static final Unit startLocationTracking$lambda$3(ParkingViewModel this$0, Location loc) {
        Intrinsics.checkNotNullParameter((Object)loc, (String)"loc");
        this$0._currentLocation.setValue((Object)loc);
        this$0.updateDaytimeState();
        BuildersKt.launch$default((CoroutineScope)ViewModelKt.getViewModelScope((ViewModel)((ViewModel)this$0)), (CoroutineContext)((CoroutineContext)Dispatchers.getIO()), null, (Function2)((Function2)new Function2<CoroutineScope, Continuation<? super Unit>, Object>(this$0, loc, null){
            int label;
            final /* synthetic */ ParkingViewModel this$0;
            final /* synthetic */ Location $loc;
            {
                this.this$0 = $receiver;
                this.$loc = $loc;
                super(2, $completion);
            }

            /*
             * Unable to fully structure code
             */
            public final Object invokeSuspend(Object $result) {
                var3_2 = IntrinsicsKt.getCOROUTINE_SUSPENDED();
                switch (this.label) {
                    case 0: {
                        ResultKt.throwOnFailure((Object)$result);
                        this.label = 1;
                        v0 = LocationHelper.INSTANCE.getAddressFromCoordinates((Context)this.this$0.getApplication(), this.$loc.getLatitude(), this.$loc.getLongitude(), (Continuation<? super String>)((Continuation)this));
                        if (v0 == var3_2) {
                            return var3_2;
                        }
                        ** GOTO lbl13
                    }
                    case 1: {
                        ResultKt.throwOnFailure((Object)$result);
                        v0 = $result;
lbl13:
                        // 2 sources

                        addr = (String)v0;
                        ParkingViewModel.access$get_currentAddress$p(this.this$0).setValue((Object)addr);
                        return Unit.INSTANCE;
                    }
                }
                throw new IllegalStateException("call to 'resume' before 'invoke' with coroutine");
            }

            public final Continuation<Unit> create(Object value, Continuation<?> $completion) {
                return (Continuation)new /* invalid duplicate definition of identical inner class */;
            }

            public final Object invoke(CoroutineScope p1, Continuation<? super Unit> p2) {
                return (this.create(p1, p2)).invokeSuspend(Unit.INSTANCE);
            }
        }), (int)2, null);
        return Unit.INSTANCE;
    }

    public static final /* synthetic */ MutableStateFlow access$get_isGpsRefreshing$p(ParkingViewModel $this) {
        return $this._isGpsRefreshing;
    }

    public static final /* synthetic */ MutableStateFlow access$get_currentLocation$p(ParkingViewModel $this) {
        return $this._currentLocation;
    }

    public static final /* synthetic */ MutableStateFlow access$get_currentAddress$p(ParkingViewModel $this) {
        return $this._currentAddress;
    }

    public static final /* synthetic */ ParkingRepository access$getRepository$p(ParkingViewModel $this) {
        return $this.repository;
    }

    public static final /* synthetic */ MutableStateFlow access$get_selectedTab$p(ParkingViewModel $this) {
        return $this._selectedTab;
    }

    public static final /* synthetic */ CompassSensorManager access$getCompassSensorManager$p(ParkingViewModel $this) {
        return $this.compassSensorManager;
    }

    public static final /* synthetic */ MutableStateFlow access$get_hapticProfile$p(ParkingViewModel $this) {
        return $this._hapticProfile;
    }

    public static final /* synthetic */ MutableStateFlow access$get_activeBtProximityDevice$p(ParkingViewModel $this) {
        return $this._activeBtProximityDevice;
    }

    public static final /* synthetic */ MutableStateFlow access$get_btProximityRssi$p(ParkingViewModel $this) {
        return $this._btProximityRssi;
    }

    public static final /* synthetic */ MutableStateFlow access$get_btProximityDistanceMeters$p(ParkingViewModel $this) {
        return $this._btProximityDistanceMeters;
    }

    public static final /* synthetic */ MutableStateFlow access$get_btProximityRelativeAngle$p(ParkingViewModel $this) {
        return $this._btProximityRelativeAngle;
    }

    public static final /* synthetic */ MutableStateFlow access$get_timerIsRunning$p(ParkingViewModel $this) {
        return $this._timerIsRunning;
    }

    public static final /* synthetic */ MutableStateFlow access$get_timerRemainingSeconds$p(ParkingViewModel $this) {
        return $this._timerRemainingSeconds;
    }

    public static final /* synthetic */ MutableStateFlow access$get_timerAlertsTriggered$p(ParkingViewModel $this) {
        return $this._timerAlertsTriggered;
    }

    public static final /* synthetic */ MutableStateFlow access$get_timerRemindersMinutes$p(ParkingViewModel $this) {
        return $this._timerRemindersMinutes;
    }

    public static final /* synthetic */ MutableStateFlow access$get_timerIsPaused$p(ParkingViewModel $this) {
        return $this._timerIsPaused;
    }

    public static final /* synthetic */ MutableStateFlow access$get_isAlarmActive$p(ParkingViewModel $this) {
        return $this._isAlarmActive;
    }
}
