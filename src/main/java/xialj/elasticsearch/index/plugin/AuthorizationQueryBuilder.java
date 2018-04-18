package xialj.elasticsearch.index.plugin;

import java.io.IOException;
import java.util.Objects;

import org.apache.lucene.index.Term;
import org.apache.lucene.search.Query;
import org.elasticsearch.common.ParseField;
import org.elasticsearch.common.ParsingException;
import org.elasticsearch.common.io.stream.StreamInput;
import org.elasticsearch.common.io.stream.StreamOutput;
import org.elasticsearch.common.lucene.BytesRefs;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.common.xcontent.XContentParser;
import org.elasticsearch.index.query.AbstractQueryBuilder;
import org.elasticsearch.index.query.QueryShardContext;

import xialj.luence.search.AuthorizationQuery;

public class AuthorizationQueryBuilder extends AbstractQueryBuilder<AuthorizationQueryBuilder> {

	public static final String NAME = "authorization";

    private static final ParseField TERM_FIELD = new ParseField(NAME);
    private static final ParseField VALUE_FIELD = new ParseField("value");
    
    protected final String fieldName;    
    protected final Object value;
    
	/**
	 * @param field
	 *            Lucene field name for access control
	 * @param grants
	 *            a java.util.Map of String key and (Boolean or java.util.Set)
	 *            value
	 */
    protected AuthorizationQueryBuilder(StreamInput in) throws IOException {
        super(in);
        fieldName = in.readString();
        value = in.readGenericValue();
    }
    
	public AuthorizationQueryBuilder(String field, Object value) {
		this.fieldName = field;
		this.value = value;
	}
	
	public static AuthorizationQueryBuilder fromXContent(XContentParser parser) throws IOException {
        String queryName = null;
        String fieldName = null;
        Object value = null;
        String currentFieldName = null;
        XContentParser.Token token;
        while ((token = parser.nextToken()) != XContentParser.Token.END_OBJECT) {
            if (token == XContentParser.Token.FIELD_NAME) {
                currentFieldName = parser.currentName();
            } else if (token == XContentParser.Token.START_OBJECT) {
                throwParsingExceptionOnMultipleFields(NAME, parser.getTokenLocation(), fieldName, currentFieldName);
                fieldName = currentFieldName;
                while ((token = parser.nextToken()) != XContentParser.Token.END_OBJECT) {
                    if (token == XContentParser.Token.FIELD_NAME) {
                        currentFieldName = parser.currentName();
                    } else {
                        if (TERM_FIELD.match(currentFieldName)) {
                            value = parser.objectBytes();
                        } else if (VALUE_FIELD.match(currentFieldName)) {
                            value = parser.objectBytes();
                        } else if (AbstractQueryBuilder.NAME_FIELD.match(currentFieldName)) {
                            queryName = parser.text();
                        } else {
                            throw new ParsingException(parser.getTokenLocation(),
                                    "["+NAME+"] query does not support [" + currentFieldName + "]");
                        }
                    }
                }
            } else if (token.isValue()) {
                throwParsingExceptionOnMultipleFields(NAME, parser.getTokenLocation(), fieldName, parser.currentName());
                fieldName = currentFieldName;
                value = parser.objectText();
            } else if (token == XContentParser.Token.START_ARRAY) {
                throw new ParsingException(parser.getTokenLocation(), "["+NAME+"] query does not support array of values");
            }
        }

        AuthorizationQueryBuilder authQueryBuilder = new AuthorizationQueryBuilder(fieldName, value);
        if (queryName != null) {
        	authQueryBuilder.queryName(queryName);
        }
        return authQueryBuilder;
    }
	
	@Override
	protected Query doToQuery(QueryShardContext context) throws IOException {
		Query query = new AuthorizationQuery(new Term(this.fieldName, BytesRefs.toBytesRef(this.value)));
		return query;
	}

	@Override
	public String getWriteableName() {
		return NAME;
	}

	@Override
	protected void doWriteTo(StreamOutput out) throws IOException {
		// TODO Auto-generated method stub
		out.writeString(fieldName);
		out.writeGenericValue(value);
	}

	@Override
	protected void doXContent(XContentBuilder builder, Params params) throws IOException {
		builder.startObject(NAME);
		builder.startObject(fieldName);
		builder.field("value", value);
		printBoostAndQueryName(builder);
        builder.endObject();
        builder.endObject();
	}

	@Override
	protected boolean doEquals(AuthorizationQueryBuilder other) {
		return (this.fieldName.equals(other.fieldName)
				&& this.value.equals(other.value));
	}

	@Override
	protected int doHashCode() {
		return Objects.hash(fieldName, value);
	}

}
