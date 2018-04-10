package xialj.elasticsearch.index.plugin;

public class AuthorizationQueryBuilders {
	public static AuthorizationQueryBuilder AuthorizationQuery(String name, String value) {
		return new AuthorizationQueryBuilder(name, value);
	}
}
