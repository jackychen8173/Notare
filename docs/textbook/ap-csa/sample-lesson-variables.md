---
title: Storing values in variables
status: draft (sample lesson for the style guide; needs a person's review)
framework: [TO CHECK against the current AP CSA course framework: primitive types, variables, arithmetic expressions]
prerequisites: []
mistakes: [int-division-truncates, double-into-int, cents-formatting]
written-by: Claude (as a style sample), 2026-11-10
---

<!-- Every program and output below was compiled and run on JDK 21 before it was written here. -->

# Storing values in variables

You'll be able to store numbers in variables, change them, and do arithmetic with them, including the kind of division that surprises people.

## First program

A transit card starts with $10.00 on it, and each tap costs $2.55. This program taps twice:

```java
public class Main {
    public static void main(String[] args) {
        int balanceCents = 1000;
        int fareCents = 255;

        balanceCents = balanceCents - fareCents;
        balanceCents = balanceCents - fareCents;

        System.out.println("Taps: 2");
        System.out.println("Balance (cents): " + balanceCents);
    }
}
```

```
Taps: 2
Balance (cents): 490
```

Run it, then add a third tap and run it again.

## What's happening

Line 3 creates a **variable**, a named place that holds one value. `int` is its type: it holds whole numbers. `balanceCents` is its name, and `1000` is the value it starts with.

Line 6 reads the right side first: it takes the current balance, subtracts the fare, and stores the result back in `balanceCents`. The old value is gone. That's why line 7 starts from 745, not from 1000.

The balance is in cents on purpose. Money as whole cents stays exact; you'll see below what goes wrong with decimals.

## Predict, then run

Before you run this, write down the two numbers it prints.

```java
public class Main {
    public static void main(String[] args) {
        int balanceCents = 1000;
        int fareCents = 255;
        int rides = balanceCents / fareCents;
        int left = balanceCents % fareCents;
        System.out.println(rides);
        System.out.println(left);
    }
}
```

<details><summary>After you run it</summary>

```
3
235
```

`/` between two `int` values gives a whole number and drops the remainder: 1000 / 255 is 3, not 3.92. `%` gives the part that was dropped: 1000 − 3 × 255 = 235 cents left on the card.

</details>

## The bug: putting a decimal into an `int`

Here's the card written with a `double` fare:

```java
public class Main {
    public static void main(String[] args) {
        int balance = 10;
        double fare = 2.55;
        balance = balance - fare;
        System.out.println(balance);
    }
}
```

It doesn't compile:

```
Main.java:5: error: incompatible types: possible lossy conversion from double to int
        balance = balance - fare;
                          ^
1 error
```

Read it from the left: the file and line (`Main.java:5`), then what's wrong. `balance - fare` is a `double` (7.45), and an `int` can't hold the .45 without losing it, so Java refuses rather than guess. The fix isn't to force it in. Pick one unit and stick with it: here, whole cents in `int`s.

> **Exam tip:** questions often mix `int` and `double` in one expression. If either side of an operator is a `double`, the result is a `double`.

## Review this

A classmate wrote this to show the balance after a top-up. It runs. Leave a comment on any line with a problem, or say it's fine.

```java
public class Main {
    public static void main(String[] args) {
        int balanceCents = 500;
        int fareCents = 255;
        int topUpCents = 2000;

        balanceCents = balanceCents + topUpCents;
        int rides = balanceCents / fareCents;

        System.out.println("Balance: " + balanceCents / 100 + "." + balanceCents % 100);
        System.out.println("Rides left: " + rides);
    }
}
```

```
Balance: 25.0
Rides left: 9
```

<!-- Planted issue (line 10, bug, cents-formatting): the cents lose their leading zero.
     2500 cents prints "25.0" and 2505 would print "25.5" instead of "25.05".
     Model comment: "When the cents are under 10, this drops a zero: 2505 prints 25.5.
     Print a 0 first when balanceCents % 100 < 10." -->

## Write it

The card has `balanceCents` on it and a ride costs `fareCents`. Print how many more cents the rider needs to afford one more ride after using up the rides they can already pay for.

```java
public class Main {
    public static void main(String[] args) {
        int balanceCents = 1000;
        int fareCents = 255;
        // Print how many cents short the card is for one more ride.
    }
}
```

<!-- Tests: 1000/255 prints 20; 255/255 prints 255 (edge: exact multiple, nothing left);
     100/255 prints 155 (edge: can't afford a single ride).
     Reference solution: System.out.println(fareCents - balanceCents % fareCents); -->

## Check

1. `int a = 7; int b = 2;` What is the value of `a / b`?
   (A) 3.5 (B) 3 (C) 4 (D) 1

   <!-- B. A: thinks / keeps decimals for ints. C: thinks it rounds. D: confuses / with %. -->

2. Which line compiles? (A) `int x = 2.0;` (B) `double y = 2;` (C) `int z = 5 / 2.0;` (D) all three

   <!-- B: an int fits in a double. A and C put a double into an int. -->

## Recap

- A variable holds one value of one type, and assigning to it replaces the old value.
- `int / int` drops the remainder; `%` gives it back.
- Keep money in whole cents, in `int`s.
