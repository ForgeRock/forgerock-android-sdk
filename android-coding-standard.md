#  Android Kotlin Style Guide

This Guideline aim to offer industrial best practice recommendations for developers to build a quality code in Android SDK . There are excellent coding guidelines available from Android's official documentation, it is highly recommended to thoroughly review these documents.

- The [Android Kotlin style guide](https://android.github.io/kotlin-guides/style.html)
- The [Kotlin Coding Conventions](https://kotlinlang.org/docs/reference/coding-conventions.html)
- The [Android contributors style guide](https://source.android.com/source/code-style.html)
- The [Google Java Style Guide](https://google.github.io/styleguide/javaguide.html)
- The [nimble](https://nimblehq.co/compass/development/code-conventions/kotlin).


- ### Android Studio Coding Style
    We also recommend to use the Default Code Style Scheme for your development.
    To import your own scheme file, open Android Studio Settings and go to **Editor > Code Style > Kotlin**, then click the gear menu and choose **Import Scheme...**.

## Table of Contents

- [Naming](#naming)
- [Packages](#packages)
- [Classes & Interfaces](#classes--interfaces)
- [Methods](#methods)
- [Properties](#Properties)
- [Variables & Parameters](#variables--parameters)
- [Visibility Modifiers](#visibility-modifiers)
- [Fields & Variables](#fields--variables)
- [Data Type Objects](#data-type-objects)
- [Enum Classes](#enum-classes)
- [Indentation](#indentation)
- [Line Length](#line-length)
- [Vertical Spacing](#vertical-spacing)
- [Semicolons](#semicolons)
- [Getters & Setters](#getters--setters)
- [Brace Style](#brace-style)
- [When Statements](#when-statements)
- [Annotations](#annotations)
- [Type Inference](#type-inference)
- [Constants vs. Variables](#constants-vs-variables)
- [Optionals](#optionals)
- [XML Guidance](#xml-guidance)
- [Language](#language)
- [Copyright Statement](#copyright-statement)

## Naming

On the whole, naming should follow Java standards, as Kotlin is a JVM-compatible language.

## Packages

Package names are similar to Java: all __lower-case__, multiple words concatenated together, without hypens or underscores:

__BAD__:

```kotlin
com.ping.sample_widget
```

__GOOD__:

```kotlin
com.ping.samplewidget
```

## Classes & Interfaces

Written in __UpperCamelCase__. For example `PingObserver`.

## Methods

Written in __lowerCamelCase__. For example `getValue()`.

## Properties

Must be written in lowerCamelCase with descriptive names:
```
private var temporary: Int = -1
private var isValid: Boolean = false
```

Constants must be written in uppercase:
```kotlin
const val EXTRA_VALUE = 42
```


## Variables & Parameters

Written in __lowerCamelCase__.

Single character values must be avoided, except for temporary looping variables.

## Visibility Modifiers

Only include visibility modifiers if you need something other than the default of public.

**BAD:**

```kotlin
public val fooProperty = 1
private val barPrivateProperty = "private"
```

**GOOD:**

```kotlin
val fooProperty = 1
private val barPrivateProperty = "private"
```

## Access Level Modifiers

Access level modifiers should be explicitly defined for classes, methods and member variables.

## Fields & Variables

Prefer single declaration per line.

__GOOD:__

```kotlin
email: String
password: String
```

## Data Type Objects

Prefer data classes for simple data holding objects.

__BAD:__

```kotlin
class Car(val name: String) {
  override fun toString() : String {
    return "Car(name=$name)"
  }
}
```

__GOOD:__

```kotlin
data class Car(val name: String)
```

## Enum Classes

Enum classes without methods may be formatted without line-breaks, as follows:

```kotlin
private enum CompassDirection { EAST, NORTH, WEST, SOUTH }
```

## Spacing

Spacing is especially important as code needs to be easily readable as part of the tutorial.

## Indentation

Indentation is using spaces - never tabs.

## Blocks

Indentation for blocks uses 2 spaces (not the default 4):

__BAD:__

```kotlin
for (i in 0..9) {
    Log.i(TAG, "index=" + i)
}
```

__GOOD:__

```kotlin
for (i in 0..9) {
  Log.i(TAG, "index=" + i)
}
```

## Line Wraps

Indentation for line wraps should use 4 spaces (not the default 8):

__BAD:__

```kotlin
val widget: Temperature =
        Calculate(heat, cool)
```

__GOOD:__

```kotlin
val widget: Temperature =
    Calculate(heat, cool)
```

## Line Length

Line length: Lines should be no longer than 100 characters long.
Line amount per class: should be no longer than 600 lines per class.


## Vertical Spacing

There should be exactly one blank line between methods to aid in visual clarity and organization. Whitespace within methods should separate functionality, but having too many sections in a method often means you should refactor into several methods.

## Comments

When they are needed, use comments to explain **why** a particular piece of code does something. Comments must be kept up-to-date or deleted.

Avoid block comments inline with code, as the code should be as self-documenting as possible. *Exception: This does not apply to those comments used to generate documentation.*


## Semicolons

Semicolons should be avoided wherever possible in Kotlin.

__BAD__:

```kotlin
val foo = true;
if (foo) {
  execute();
}
```

__GOOD__:

```kotlin
val foo = true
if (foo) {
    execute()
}
```

## Getters & Setters

Unlike Java, direct access to fields in Kotlin is preferred.

If custom getters and setters are required, they should be declared [following Kotlin conventions](https://kotlinlang.org/docs/reference/properties.html) rather than as separate methods.

## Brace Style

Only trailing closing-braces are awarded their own line. All others appear the same line as preceding code:

__BAD:__

```kotlin
class PingClass
{
  fun doSomething()
  {
    if (someTest)
    {
      // ...
    }
    else
    {
      // ...
    }
  }
}
```

__GOOD:__

```kotlin
class PingClass {
  fun doSomething() {
    if (someTest) {
      // ...
    } else {
      // ...
    }
  }
}
```

Conditional statements are always required to be enclosed with braces, irrespective of the number of lines required.

__BAD:__

```kotlin
if (someTest)
  doSomething()
if (someTest) doSomethingElse()
```

__GOOD:__

```kotlin
if (someTest) {
  doSomething()
}
if (someTest) { doSomethingElse() }
```

## When Statements

Unlike `switch` statements in Java, `when` statements do not fall through. Separate cases using commas if they should be handled the same way. Always include the else case.

__BAD:__

```kotlin
when (anInput) {
  1 -> doSomethingForCaseOneOrTwo()
  2 -> doSomethingForCaseOneOrTwo()
  3 -> doSomethingForCaseThree()
}
```

__GOOD:__

```kotlin
when (anInput) {
  1, 2 -> doSomethingForCaseOneOrTwo()
  3 -> doSomethingForCaseThree()
  else -> println("No case satisfied")
}
```

## Type Inference

Type inference should be preferred where possible to explicitly declared types.

__BAD:__

```kotlin
val something: MyType = MyType()
val meaningOfLife: Int = 42
```

__GOOD:__

```kotlin
val something = MyType()
val meaningOfLife = 42
```

## Constants vs. Variables

Constants are defined using the `val` keyword, and variables with the `var` keyword. Always use `val` instead of `var` if the value of the variable will not change.

*Tip*: A good technique is to define everything using `val` and only change it to `var` if the compiler complains!

## Optionals

Declare variables and function return types as nullable with `?` where a `null` value is acceptable.

Use implicitly unwrapped types declared with `!!` only for instance variables that you know will be initialized before use, such as subviews that will be set up in `onCreate` for an Activity or `onCreateView` for a Fragment.

When naming nullable variables and parameters, avoid naming them like `nullableString` or `maybeView` since their nullability is already in the type declaration.

When accessing a nullable value, use the safe call operator if the value is only accessed once or if there are many nullables in the chain:

```kotlin
editText?.setText("foo")
```

## XML Guidance

Since Android uses XML extensively in addition to Java, we have some rules
specific to XML.

## XML File Names

View-based XML files should be prefixed with the type of view that they
represent.

__BAD:__

- `login.xml`
- `main_screen.xml`
- `rounded_edges_button.xml`

__GOOD:__

- `activity_login.xml`
- `fragment_main_screen.xml`
- `button_rounded_edges.xml`

## Indentation

Similarly to Java, indentation should be __two characters__.

## Use Context-Specific XML Files

Wherever possible XML resource files should be used:

- Strings => `res/values/strings.xml`
- Styles => `res/values/styles.xml`
- Colors => `res/color/colors.xml`
- Animations => `res/anim/`
- Drawable => `res/drawable`


## XML Attribute Ordering

Where appropriate, XML attributes should appear in the following order:

- `id` attribute
- `layout_*` attributes
- style attributes such as `gravity` or `textColor`
- value attributes such as `text` or `src`

Within each of these groups, the attributes should be ordered alphabetically.

## Language

Use `en-US` English spelling.

__BAD:__

```kotlin
val colourValue = "red"
```

__GOOD:__

```kotlin
val colorValue = "red"
```

## Copyright Statement

The following copyright statement should be included at the top of every source file:

```
/*
 * Copyright (c) 2024 ForgeRock. All rights reserved.
 *
 * This software may be modified and distributed under the terms
 * of the MIT license. See the LICENSE file for details.
 */
 ```

## Documentation comments

For longer documentation comments, place the opening /** on a separate line and begin each subsequent line with an asterisk:
```
   /**
   * This is a documentation comment
   * on multiple lines.
   */ 
```
Short comments can be placed on a single line:
```
/** This is a short documentation comment. */
```

Generally, avoid using @param and @return tags. Instead, incorporate the description of parameters and return values directly into the documentation comment, and add links to parameters wherever they are mentioned. Use @param and @return only when a lengthy description is required which doesn't fit into the flow of the main text.

 __BAD:__
 ```  
  /**
   * Returns the absolute value of the given number.
   * @param number The number to return the absolute value for.
   * @return The absolute value.
   */
    fun abs(number: Int): Int { /*...*/ }
 ```

__GOOD:__ 
```
    /**
    * Returns the absolute value of the given [number].
      */
      fun abs(number: Int): Int { /*...*/ }
```

## General Code Standards to Consideration

1. Use default parameters for dependency injection
2. Don't ignore exceptions
     - Catch specific exceptions over generic exception
3. Minimal comments that are helpful (prioritize understandable code over the need to explain in comments).
     - provide links to issue trackers if relevant.
     - Don't leave commented out code use TODO comments.
     - Add your initials to the comments left.
4. Don't use deprecated methods (There are exceptions explain why in comment and link)
5. Keep logic out of Fragments and Activities that is not view based.
6. Don't loosen scoping for unit testing For example: internal val foo or public val bar (For example if you want to mock the foo or bar , don't make it public, try to inject)
7. Avoid redundant code
8. All new code is written in Kotlin
     - When changes need to be made to a Java class, convert it to Kotlin. 
9. When interacting with a dependency, check for any updates. We don't want to be a year out of date.
10. Make sure that you are not on the main thread when performing any non-ui work. Only use the main thread to update UI
11. Please avoid multiple return points from methods whenever possible; balance this with legibility and cleanliness.
12. In Android eco system do not hold a hard reference to anything that is lifecycle aware.
     - Activities / Context / Fragment / CustomViews
     - this causes memory leaks or unintentional memory retention, if you prefer ..
13. When possible avoid placing code in the `onResume`/`onStart`/`onPause`/`onStop` methods of fragments and activities
     - once one these type of classes are created it should be able to act autonomously until destroyed.
     - placing code in these methods can have subtle side effects that will be hard to see in the lifecycle of the application.
     - These are generally user visible methods as well and any computations performed in them can potentially slow the users experience to a noticeable point.
14. `public`, `protected`/`internal`, `private` from top to bottom or bottom to top of the file. But have a team agreement . This includes functions and properties/fields
     - `overrides` should be grouped together and then sub-grouped by the interface/abstract class they implement
     - properties/functions/methods camelCase
     - resources snake_case e.g. `R.string.string_resource_of_ok`, synthetic resource `foo_text_edit`
     - constants UPPER_SNAKE_CASE
     - classes/interfaces/files names PascalCase names and the name of the file




