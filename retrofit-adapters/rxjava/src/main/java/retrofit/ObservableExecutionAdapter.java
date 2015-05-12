package retrofit;

import java.io.IOException;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import rx.Observable;
import rx.Subscriber;
import rx.functions.Action0;
import rx.functions.Func1;
import rx.subscriptions.Subscriptions;

/**
 * TODO docs
 */
public final class ObservableExecutionAdapter implements CallAdapter {
  /**
   * TODO
   */
  private static ObservableExecutionAdapter create() {
    return new ObservableExecutionAdapter();
  }

  private ObservableExecutionAdapter() {
  }

  @Override public Type parseMessageType(Type returnType) {
    if (Types.getRawType(returnType) != Observable.class) {
      return null;
    }
    if (returnType instanceof ParameterizedType) {
      return Types.getParameterUpperBound((ParameterizedType) returnType);
    }
    throw new IllegalStateException("Observable return type must be parameterized"
        + " as Observable<Foo> or Observable<? extends Foo>");
  }

  @Override public <T> Observable<?> adapt(final Call<T> call, final Packaging packaging) {
    Observable<Response<T>> responseObservable = Observable.create(new CallOnSubcribe<T>(call));
    switch (packaging) {
      case RESPONSE:
        return responseObservable;

      case RESULT:
        return responseObservable //
            .map(new Func1<Response<T>, Result<T>>() {
              @Override public Result<T> call(Response<T> response) {
                return Result.fromResponse(response);
              }
            })
            .onErrorReturn(new Func1<Throwable, Result<T>>() {
              @Override public Result<T> call(Throwable throwable) {
                return Result.fromError(throwable);
              }
            });

      case NONE:
        return responseObservable //
            .flatMap(new Func1<Response<T>, Observable<T>>() {
              @Override public Observable<T> call(Response<T> response) {
                if (response.isSuccess()) {
                  return Observable.just(response.body());
                }
                return Observable.error(new IOException()); // TODO non-suck message.
              }
            });

      default:
        throw new AssertionError();
    }
  }

  private static final class CallOnSubcribe<T> implements Observable.OnSubscribe<Response<T>> {
    private final Call<T> originalCall;

    private CallOnSubcribe(Call<T> originalCall) {
      this.originalCall = originalCall;
    }

    @Override public void call(final Subscriber<? super Response<T>> subscriber) {
      // Since Call is a one-shot type, clone it for each new subscriber.
      final Call<T> call = originalCall.clone();

      // Attempt to cancel the call if it is still in-flight on unsubscription.
      subscriber.add(Subscriptions.create(new Action0() {
        @Override public void call() {
          call.cancel();
        }
      }));

      call.enqueue(new Callback<T>() {
        @Override public void success(Response<T> response) {
          if (subscriber.isUnsubscribed()) {
            return;
          }
          try {
            subscriber.onNext(response);
          } catch (Throwable t) {
            subscriber.onError(t);
            return;
          }
          subscriber.onCompleted();
        }

        @Override public void failure(Throwable t) {
          if (subscriber.isUnsubscribed()) {
            return;
          }
          subscriber.onError(t);
        }
      });
    }
  }
}
