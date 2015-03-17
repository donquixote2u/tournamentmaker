package data.team.modifier;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface CreatableTeamModifier {
	String displayName();
	String[] parameters() default {};
	int minSize() default 1;
	int maxSize() default Integer.MAX_VALUE;
}
