package janala.interpreters;

import static janala.interpreters.ObjectValue.ADDRESS_UNKNOWN;

import janala.config.Config;
import janala.solvers.History;

public final class StaticInvocation {
  private final Config config;
  
  public StaticInvocation(Config config) {
    this.config = config;
  }
  
  private boolean knownMethod = false;
  
  public boolean getKnownMethod() {
    return knownMethod;
  }

  public Value invokeMethod(int iid, String owner, String name, Value[] args, History history) {
    knownMethod = true;
    if (owner.equals("java/lang/Integer") && name.equals("valueOf")) {
      if (args[0] instanceof IntValue) {
        IntValue intValue = (IntValue) args[0];
        return new IntegerObjectValue(intValue, ADDRESS_UNKNOWN); 
      } 
    } else if (owner.equals("java/lang/Long") && name.equals("valueOf")) {
      if (args[0] instanceof LongValue) {
        LongValue longValue = (LongValue) args[0];
        return new LongObjectValue(longValue, ADDRESS_UNKNOWN);
      }
    } else if (owner.equals("java/sql/Date") && name.equals("valueOf")) {
      SqlDateObjectValue ret = new SqlDateObjectValue();
      if (args[0] instanceof StringValue) {
        ret.longValue =
            new LongValue((java.sql.Date.valueOf(((StringValue) args[0]).getConcrete()).getTime()));
        return ret;
      }
    } else if (owner.equals("java/sql/Time") && name.equals("valueOf")) {
      SqlDateObjectValue ret = new SqlDateObjectValue();
      if (args[0] instanceof StringValue) {
        ret.longValue =
            new LongValue((java.sql.Time.valueOf(((StringValue) args[0]).getConcrete()).getTime()));
        return ret;
      }
    } else if (owner.equals("java/sql/Timestamp") && name.equals("valueOf")) {
      SqlDateObjectValue ret = new SqlDateObjectValue();
      if (args[0] instanceof StringValue) {
        ret.longValue =
            new LongValue(
                (java.sql.Timestamp.valueOf(((StringValue) args[0]).getConcrete()).getTime()));
        return ret;
      }
    } else if (owner.equals("janala/Main") && name.equals("assume") && args.length == 1) {
      if (((IntValue) args[0]).concrete != 0) {
        history.setLastBranchDone();
      }
      return PlaceHolder.instance;
    } else if (owner.equals("janala/Main") && name.equals("forceTruth") && args.length == 1) {
      history.setLastForceTruth();
      return PlaceHolder.instance;
    } else if (owner.equals("janala/Main") && name.equals("MakeSymbolic") && args.length == 1) {
      int symbol = args[0].MAKE_SYMBOLIC(history);
      history.addInput(symbol, args[0]);
      return PlaceHolder.instance;
    } else if (owner.equals("janala/Main") && name.equals("beginScope") && 
      (args == null || args.length == 0)) {
      history.addInput(config.scopeBeginSymbol, null);
      history.beginScope(iid);
      return PlaceHolder.instance;
    } else if (owner.equals("janala/Main") && name.equals("endScope") && 
      (args == null || args.length == 0)) {
      history.addInput(config.scopeEndSymbol, null);
      history.endScope(iid);
      return PlaceHolder.instance;
    } else if (owner.equals("janala/Main")
        && name.equals("abstractEqualsConcrete")
        && args.length == 1) {
      history.abstractData(((IntValue) args[0]).concrete != 0);
      return PlaceHolder.instance;
    } else if (owner.equals("janala/Main") && name.equals("assumeOrBegin") && args.length == 1) {
      return history.assumeOrBegin((IntValue) args[0]);
    } else if (owner.equals("janala/Main") && name.equals("assumeOr") && args.length == 2) {
      return history.assumeOr((IntValue) args[0], (SymbolicOrValue) args[1]);
    } else if (owner.equals("janala/Main") && name.equals("assumeOrEnd") && args.length == 1) {
      return history.assumeOrEnd(iid, (SymbolicOrValue) args[0]);
     } else if (owner.equals("janala/Main") && name.equals("ignore") && args.length == 0) {
      return history.ignore();
    }
    knownMethod = false;
    return PlaceHolder.instance;
  }
}
