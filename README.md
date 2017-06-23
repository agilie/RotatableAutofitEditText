## RotatableAutofitEditText

[ ![Download](https://api.bintray.com/packages/agilie/maven/RotatableAutofitEditText/images/download.svg) ](https://bintray.com/agilie/maven/RotatableAutofitEditText/_latestVersion)

![Screenshot1](http://i.imgur.com/gwBiu3E.gif)

## What is RotatableAutofitEditText?

RotatableAutofitEditText is a lightweight open-source library that allows user to move, rotate, and resize text field at the same time. If you need such functionality in your project, we're happy to save some time for you so you can drink more beer with your friends :)

# Usage

### Gradle

Add dependency in your `build.gradle` file:
````gradle
compile 'com.agilie:rotatable-autofit-edittext:1.2'
````

### Maven
Add rependency in your `.pom` file:
````xml
<dependency>
  <groupId>com.agilie</groupId>
  <artifactId>rotatable-autofit-edittext</artifactId>
  <version>1.2</version>
  <type>pom</type>
</dependency>
````

## How to use

Simply use *RotatableAutofitEditText* instead of familiar to all Android devs [EditText](https://developer.android.com/reference/android/widget/EditText.html) component programmatically or in your *xml* files:

```xml
<com.agilie.RotatableAutofitEditText 
    xmlns:android="http://schemas.android.com/apk/res/android"
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

Here's a list of available properties, feel free to customize them according to your wishes and requirements:

```java
    maxTextSize     // sets maximum text size
    minTextSize     // sets minimum text size 
    minWidth        // sets minimum EditText width
    movable         // true if EditText must move in parent view
    resizable       // true if EditText can be resized with pinch
    rotatable       // true if EditText can be rotated
    clipBounds      // true if EditText must not move out of parent view bounds
```

Library also supports usage of different [Typefaces](https://developer.android.com/reference/android/graphics/Typeface.html) as well.

Also you can clone this project and compile [sample](sample/) module to test our library in action. 

## Requirements

Android 3.0+ (API level 11+)

## Troubleshooting

Problems? Check the [Issues](https://github.com/agilie/RotatableAutofitEditText/issues) block
to find the solution or create an new issue that we will fix asap. 

## Author

This library is open-sourced by [Agilie Team](https://www.agilie.com) <info@agilie.com>

## Contributors

- [Denis Bogoslovcev](https://github.com/anonymous265)
- [Roman Kapshuk](https://github.com/RomanKapshuk)

## Contact us
If you have any questions, suggestions or just need a help with web or mobile development, please email us at<br/> <android@agilie.com><br/>
You can ask us anything from basic to complex questions. <br/>
We will continue publishing new open-source projects. Stay with us, more updates will follow!<br/>

## License

The [MIT](LICENSE.md) License (MIT) Copyright © 2017 [Agilie Team](https://www.agilie.com)
