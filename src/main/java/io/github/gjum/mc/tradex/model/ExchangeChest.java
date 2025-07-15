package io.github.gjum.mc.tradex.model;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;

///  A block that contains multiple Exchanges
public class ExchangeChest {
	public final ArrayList<@Nullable Exchange> list = new ArrayList<>(1);

	public void add(@NotNull Exchange exchange) {
		list.ensureCapacity(exchange.multi);
		while (list.size() < exchange.multi) list.add(null);
		list.set(exchange.index - 1, exchange);
	}
}
