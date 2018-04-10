package xialj.elasticsearch.index.plugin;

import java.util.Collections;
import java.util.List;

import org.elasticsearch.plugins.Plugin;
import org.elasticsearch.plugins.SearchPlugin;

public class AuthorizationQueryPlugin extends Plugin implements SearchPlugin {
	@Override
	public List<QuerySpec<?>> getQueries() {
		return Collections.singletonList(new QuerySpec<>(AuthorizationQueryBuilder.NAME, AuthorizationQueryBuilder::new,
				AuthorizationQueryBuilder::fromXContent));
	}
}
