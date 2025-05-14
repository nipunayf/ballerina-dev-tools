boolean moduleFlag1 = false;
boolean moduleFlag2 = true;

() var1 = moduleFlag1 ? true : false;
int var4 = moduleFlag1 ? "True" : 0;

string? moduleNullableStr = ();
int? moduleNullableInt = 1;

() var5 = moduleNullableStr ?: "False";
int var7 = moduleNullableStr ?: moduleNullableInt ?: false;
int val = "hello";