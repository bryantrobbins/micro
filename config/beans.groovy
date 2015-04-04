// Define beans here using the BeanBuilder DSL

import com.btr3.demo.controllers.HelloController

beans {
    helloController(HelloController) {
			companyName = "Happysoft"
    }
}
