Change Log
==========

Bugfix Issue #2 *(2016-01-14)*
----------------------------

Fixes #2
Minimum scale never properly set when calling com.lyft.android.scissors.TouchManager.resetFor(...)
Details:
- setMinimumScale(...) in resetFor() prematurely called before this.bitmapWidth/Height were set.
- computeLimit(...) in resetFor() called without proper scaling.
- setMinimumScale(...) method was erroneously always set to 1.0f, when bitmap was smaller than viewport.


Version 1.0.1 *(2015-11-19)*
----------------------------

Add Add proguard rules for optional dependencies.


Version 1.0.0 *(2015-11-17)*
----------------------------

Initial release.
