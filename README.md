## RotatableAutofitEditText
Extended EditText which allows to move, rotate and resize text at the same time

## Sample Screen Recording
![Screenshot1](http://i.imgur.com/gwBiu3E.gif)

## Download

- <b>Gradle</b>
```java
compile 'com.agilie:rotatable-autofit-edittext:1.2'
```

## How to use
```xml
<com.agilie.RotatableAutofitEditText xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:autofit="http://schemas.android.com/apk/res-auto"
    android:id="@+id/autoResizeEditText"
    android:layout_width="wrap_content"
    android:layout_height="wrap_content"
    android:background="@drawable/rounded_corners_white_transparent_50"
    android:gravity="center"
    android:hint="@string/hint_add_some_text"
    android:padding="16dp"
    android:textColor="@android:color/white"
    android:textColorHint="@android:color/darker_gray"
    autofit:clipBounds="true"
    autofit:maxTextSize="@dimen/autoresize_max_text_size"
    autofit:minTextSize="@dimen/autoresize_min_text_size"
    autofit:minWidth="@dimen/autoresize_min_width"
    autofit:movable="true"
    autofit:resizable="true"
    autofit:rotatable="true" />
```

```java
autofit:maxTextSize - sets maximum text size
autofit:minTextSize - sets minimum text size 
autofit:minWidth - sets minimum EditText width
autofit:movable - true if EditText must move in parent view
autofit:resizable - true if EditText can be resized with pinch
autofit:rotatable - true if EditText can be rotated
autofit:clipBounds - true if EditText must not move out of parent view bounds
```

## Requirements

Android 3.0+ (API level 11+)

## Author

This library is open-sourced by [Agilie Team](https://www.agilie.com) <info@agilie.com>

## License

The [MIT](LICENSE.md) License (MIT) Copyright © 2017 [Agilie Team](https://www.agilie.com)
