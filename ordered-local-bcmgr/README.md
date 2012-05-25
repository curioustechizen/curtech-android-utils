The aim of this project is to enhance the `LocalBroadcastManager` class from the Android support library. In particular, the objective is to add the capability to send ordered broadcasts locally.

Unfortunately, the `LocalBroadcastManager` class implementation has lots of `private` and `final` members and methods, which means that class cannot be extended easily. `OrderEnabledLocalBroadcastManager` is thus a re-implementation of `LocalBroadcastManager`, even though they share a lot of the code.

Another point to note is that regular `BroadcastReceiver`s cannot be used in conjunction with `OrderEnabledLocalBroadcastManager`. To understand why this is so, please look at [this discussion](https://groups.google.com/d/topic/android-developers/drDGOuml-qo/discussion) with Ms. Dianne Hackborn on android-developers google group.

You need to register instances of `LocalBroadcastReceiver` if you wish to be able to consume an ordered local broadcast.

####What's implemented?

- `sendOrderedBroadcast(Intent)`
- `consumeBroadcast()` - analogous to `abortBroadcast()`
- `isBroadcastConsumed()` - analogous to `getAbortBroadcast()`
- `clearConsumeBroadcast()` - analogous to `clearAbortBroadcast()`

####What needs to be implemented?

- Other variants of `sendOrderedBroadcast`, in particular one which takes a resultReceiver as argument.
- Ability to set and get result data in the `LocalBroadcastReceiver`. In other words, the `setResult*` and `getResult*` methods.


####Acknowledgements
I wouldn't even have started writing this library if it weren't for advice from [Mark Murphy](http://commonsware.com/mmurphy) of commonsware fame. Throughout the implementation too, I constantly received tips from him. Thanks, Mark!
